package uk.gov.di.ipv.cri.kbv.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class TempCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempCleaner.class);

    @ExcludeFromGeneratedCoverageReport
    private TempCleaner() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Pattern[] FILE_PATTERNS_TO_DELETE =
            new Pattern[] {Pattern.compile("^core\\..*"), Pattern.compile("^hsperfdata_.*")};

    public static void clean() {
        clean(new File(System.getProperty("java.io.tmpdir", "/tmp")));
    }

    static void clean(File tmpDir) {
        File[] files = tmpDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.isFile()) continue;

            for (Pattern pattern : FILE_PATTERNS_TO_DELETE) {
                if (pattern.matcher(file.getName()).matches()) {
                    try {
                        Files.delete(file.toPath());
                        LOGGER.info("Deleted file from /tmp during init: {}", file.getName());
                    } catch (IOException e) {
                        LOGGER.warn(
                                "Deleting file from /tmp failed during init: {}", file.getName());
                    }
                    break;
                }
            }
        }
    }
}
