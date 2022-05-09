package uk.gov.di.ipv.cri.experian.kbv.api.annotations;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SystemPropertiesExtension implements AfterEachCallback, BeforeEachCallback {

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        SystemProperties annotation =
                extensionContext.getTestMethod().get().getAnnotation(SystemProperties.class);
        System.clearProperty(annotation.key());
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        SystemProperties annotation =
                extensionContext.getTestMethod().get().getAnnotation(SystemProperties.class);
        System.setProperty(annotation.key(), annotation.value());
    }
}
