package uk.gov.di.ipv.cri.experian.kbv.api.annotations;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(SystemPropertiesExtension.class)
public @interface SystemProperties {

    String key();

    String value();
}
