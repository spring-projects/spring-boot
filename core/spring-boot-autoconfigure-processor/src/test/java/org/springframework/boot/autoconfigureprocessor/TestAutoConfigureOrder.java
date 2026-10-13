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
 * Alternative to Spring Boot's {@code @AutoConfigureOrder} for testing (removes the need
 * for a dependency on the real annotation).
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface TestAutoConfigureOrder {

	int value() default Integer.MAX_VALUE;

}
