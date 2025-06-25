package uk.gov.di.ipv.cri.kbv.healthcheck.util.bash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.healthcheck.exceptions.ProcessInvocationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Bash {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bash.class);
    private static final String BASH_PATH = "/bin/bash";
    private static final String BASH_OPTION = "-c";

    private Bash() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String execute(String command) {
        return execute(command, BashCommandConfiguration.defaultConfig());
    }

    public static String execute(String command, BashCommandConfiguration config) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }

        Process process = null;

        try {
            LOGGER.debug("Executing command: {}", command);

            ProcessBuilder pb =
                    new ProcessBuilder(BASH_PATH, BASH_OPTION, command).redirectErrorStream(true);

            pb.environment().putAll(config.getEnvironmentVariables());
            process = pb.start();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                boolean completed =
                        process.waitFor(config.getTimeout().toSeconds(), TimeUnit.SECONDS);

                String output = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                if (!completed) {
                    destroyProcess(process);
                    throw new ProcessInvocationException(
                            "Command execution timed out after "
                                    + config.getTimeout().toSeconds()
                                    + " seconds");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String errorMessage = parseExitCode(exitCode);
                    LOGGER.error("Command failed with exit code {}: {}", exitCode, errorMessage);
                    throw new ProcessInvocationException(
                            "Command execution failed with exit code "
                                    + exitCode
                                    + ": "
                                    + errorMessage);
                }

                LOGGER.debug("Command completed successfully with output: {}", output);
                return output.trim();
            }
        } catch (IOException e) {
            throw new ProcessInvocationException("IO error during command execution", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessInvocationException("Command execution interrupted", e);
        } finally {
            destroyProcess(process);
        }
    }

    private static void destroyProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for process cleanup");
                Thread.currentThread().interrupt();
            }
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static String parseExitCode(int exitCode) {
        return switch (exitCode) {
            case 1 -> "General error, invalid syntax";
            case 2 -> "Misuse of shell builtins";
            case 126 -> "Permission denied - command cannot execute";
            case 127 -> "Command not found";
            case 128 -> "Invalid argument to exit";
            case 129 -> "Invalid exit argument";
            case 130 -> "Terminated by Ctrl+C";
            case 137 -> "Killed by SIGKILL";
            case 139 -> "Segmentation fault";
            case 143 -> "Terminated by SIGTERM";
            case 255 -> "Exit status out of range";
            case 134 -> "Aborted (core dumped)";
            case 131 -> "Terminated by SIGPIPE";
            case 124 -> "Command timed out";
            default -> "Unknown error (exit code: " + exitCode + ")";
        };
    }
}
