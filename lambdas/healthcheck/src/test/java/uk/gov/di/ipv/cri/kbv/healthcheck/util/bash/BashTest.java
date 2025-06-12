package uk.gov.di.ipv.cri.kbv.healthcheck.util.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedConstruction;
import uk.gov.di.ipv.cri.kbv.healthcheck.exceptions.ProcessInvocationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class BashTest {

    @Test
    void shouldReturnOutputWhenCommandSucceeds() {
        String output = Bash.execute("echo hello");
        assertEquals("hello", output);
    }

    @Test
    void shouldThrowExceptionForEmptyCommand() {
        Executable exec = () -> Bash.execute("");
        assertThrows(IllegalArgumentException.class, exec);
    }

    @Test
    void shouldThrowProcessInvocationExceptionForInvalidCommand() {
        Executable exec = () -> Bash.execute("non_existing_command");
        ProcessInvocationException exception = assertThrows(ProcessInvocationException.class, exec);
        assertTrue(exception.getMessage().contains("Command execution failed"));
    }

    @Test
    void shouldTimeOut() {
        BashCommandConfiguration config =
                new BashCommandConfiguration(Duration.ofSeconds(5), Map.of());
        Executable exec = () -> Bash.execute("sleep 15", config);
        ProcessInvocationException exception = assertThrows(ProcessInvocationException.class, exec);
        assertTrue(exception.getMessage().contains("Command execution timed out after 5 seconds"));
    }

    @Test
    public void testIOExceptionDuringCommandExecution() {
        try (MockedConstruction<ProcessBuilder> ignored =
                mockConstruction(
                        ProcessBuilder.class,
                        (builder, context) -> {
                            when(builder.redirectErrorStream(true)).thenReturn(builder);
                            when(builder.start()).thenThrow(new IOException("Mock IO failure"));
                        })) {

            ProcessInvocationException ex =
                    assertThrows(
                            ProcessInvocationException.class, () -> Bash.execute("echo Hello"));

            assertTrue(ex.getMessage().contains("IO error during command execution"));
        }
    }

    @Test
    public void testInterruptedExceptionDuringCommandExecution() throws InterruptedException {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(mockProcess.waitFor(anyLong(), any())).thenThrow(new InterruptedException());

        try (MockedConstruction<ProcessBuilder> ignored =
                mockConstruction(
                        ProcessBuilder.class,
                        (builder, context) -> {
                            when(builder.redirectErrorStream(true)).thenReturn(builder);
                            when(builder.start()).thenReturn(mockProcess);
                        })) {

            ProcessInvocationException ex =
                    assertThrows(
                            ProcessInvocationException.class,
                            () -> {
                                Bash.execute("echo Hello");
                            });

            assertTrue(ex.getMessage().contains("Command execution interrupted"));
        }
    }
}
