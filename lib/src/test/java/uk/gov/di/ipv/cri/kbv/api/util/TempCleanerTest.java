package uk.gov.di.ipv.cri.kbv.api.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempCleanerTest {

    private final File testDir =
            new File(System.getProperty("java.io.tmpdir"), "temp-cleaner-test");

    @AfterEach
    void cleanUp() {
        if (testDir != null && testDir.isDirectory()) {
            for (File file : testDir.listFiles()) {
                if (file != null && file.exists()) {
                    file.delete();
                }
            }
            testDir.delete();
        } else if (testDir != null && testDir.isFile()) {
            testDir.delete();
        }
    }

    @Test
    void testDeletesMatchingFiles() throws IOException {
        testDir.mkdirs();

        File coreDump = new File(testDir, "core.java.10");
        File perfData = new File(testDir, "hsperfdata_sbx_user");

        coreDump.createNewFile();
        perfData.createNewFile();

        File xrayFile = new File(testDir, ".aws-xray");
        File crtFile = new File(testDir, "aws-crt-java-xyz123");
        String keyStoreFileName = UUID.randomUUID() + ".tmp";
        File keyStoreFile = new File(testDir, keyStoreFileName);

        xrayFile.createNewFile();
        crtFile.createNewFile();
        keyStoreFile.createNewFile();

        TempCleaner.clean(testDir);

        assertFalse(coreDump.exists());
        assertFalse(perfData.exists());

        assertTrue(xrayFile.exists());
        assertTrue(crtFile.exists());
        assertTrue(keyStoreFile.exists());
    }

    @Test
    void doesNotThrowOnEmptyTempDir() {
        testDir.mkdirs();
        assertDoesNotThrow(() -> TempCleaner.clean(testDir));
    }

    @Test
    void doesNotThrowOnInvalidTempDir() throws IOException {
        testDir.createNewFile();
        assertDoesNotThrow(() -> TempCleaner.clean(testDir));
    }

    @Test
    void doesNotThrowWhenDirInTemp() {
        testDir.mkdirs();

        File dir = new File(testDir, "temp-dir");
        dir.mkdirs();

        assertDoesNotThrow(() -> TempCleaner.clean(testDir));

        assertTrue(dir.exists());
    }

    @Test
    void doesNotThrowOnIOExceptionDeletingFile() throws IOException {
        testDir.mkdirs();
        File coreDumpFile = new File(testDir, "core.java.10");
        coreDumpFile.createNewFile();
        coreDumpFile.getPath();
        Path path = Paths.get(coreDumpFile.getPath());

        try (MockedStatic<Files> mocked = Mockito.mockStatic(Files.class)) {
            mocked.when(() -> Files.delete(path)).thenThrow(new IOException("Mock delete failure"));

            assertDoesNotThrow(() -> TempCleaner.clean(testDir));
            assertTrue(coreDumpFile.exists());
        }
    }
}
