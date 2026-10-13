/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alternative to Spring Boot's {@code @AutoConfigureBefore} for testing (removes the need
 * for a dependency on the real annotation).
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Documented
public @interface TestAutoConfigureBefore {

	Class<?>[] value() default {};

	String[] name() default {};

}
