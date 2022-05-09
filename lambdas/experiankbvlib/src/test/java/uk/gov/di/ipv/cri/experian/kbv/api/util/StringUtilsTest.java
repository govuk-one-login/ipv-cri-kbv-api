package uk.gov.di.ipv.cri.experian.kbv.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.di.ipv.cri.experian.kbv.api.util.StringUtils.isNotBlank;

class StringUtilsTest {
    @Test
    void whenIsNotBlankIsCalledWithNullReturnFalse() {
        assertFalse(isNotBlank(null));
    }

    @Test
    void whenIsNotBlankIsCalledWithBlankReturnFalse() {
        assertFalse(isNotBlank("  "));
    }

    @Test
    void whenIsNotBlankIsCalledWithEmptyStringReturnFalse() {
        assertFalse(isNotBlank(""));
    }
}
