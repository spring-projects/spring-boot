/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ConditionalOnClass} analogue for architecture checks.
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestConditionalOnClass {

	Class<?>[] value() default {};

	String[] name() default {};

}
