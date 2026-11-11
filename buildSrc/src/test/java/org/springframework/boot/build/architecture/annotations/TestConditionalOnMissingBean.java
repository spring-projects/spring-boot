/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ConditionalOnMissingBean} analogue for architecture checks.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestConditionalOnMissingBean {

	Class<?>[] value() default {};

	String[] type() default {};

	Class<?>[] ignored() default {};

	String[] ignoredType() default {};

	Class<? extends Annotation>[] annotation() default {};

	String[] name() default {};

	Class<?>[] parameterizedContainer() default {};

}
