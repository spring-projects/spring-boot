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
 * Fake {@code @ConditionalOnClass} annotation used for testing.
 *
 * @author Madhura Bhave
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestConditionalOnClass {

	Class<?>[] value() default {};

	String[] name() default {};

}
