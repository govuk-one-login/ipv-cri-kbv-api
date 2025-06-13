package uk.gov.di.ipv.cri.kbv.healthcheck.util.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.gov.di.ipv.cri.kbv.healthcheck.exceptions.ProcessInvocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
