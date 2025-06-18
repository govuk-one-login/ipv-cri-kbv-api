package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FailReportTest {

    @Test
    void shouldNotPassOnException() {
        Exception exception = new Exception("test");
        Report failReport = new FailReport(exception);
        Map<String, Map<String, Object>> attributes = failReport.getAttributes();

        assertFalse(failReport.isPassed());
        assertEquals(1, attributes.size());
        assertNotNull(attributes.get("exception"));
        assertEquals("Exception: test", attributes.get("exception").get("message"));
    }
}
