package uk.gov.di.ipv.cri.kbv.api.utils.bash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.exceptions.ProcessInvocationException;

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

                String output = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                boolean completed =
                        process.waitFor(config.getTimeout().toSeconds(), TimeUnit.SECONDS);

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
        switch (exitCode) {
            case 1:
                return "General error, invalid syntax";
            case 2:
                return "Misuse of shell builtins";
            case 126:
                return "Permission denied - command cannot execute";
            case 127:
                return "Command not found";
            case 128:
                return "Invalid argument to exit";
            case 129:
                return "Invalid exit argument";
            case 130:
                return "Terminated by Ctrl+C";
            case 137:
                return "Killed by SIGKILL";
            case 139:
                return "Segmentation fault";
            case 143:
                return "Terminated by SIGTERM";
            case 255:
                return "Exit status out of range";
            case 134:
                return "Aborted (core dumped)";
            case 131:
                return "Terminated by SIGPIPE";
            case 124:
                return "Command timed out";
            default:
                return "Unknown error (exit code: " + exitCode + ")";
        }
    }
}
