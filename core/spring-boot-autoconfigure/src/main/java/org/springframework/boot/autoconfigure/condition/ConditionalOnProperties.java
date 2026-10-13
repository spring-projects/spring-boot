/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Container annotation that aggregates several
 * {@link ConditionalOnProperty @ConditionalOnProperty} annotations.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see ConditionalOnProperty
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnPropertyCondition.class)
public @interface ConditionalOnProperties {

	/**
	 * Return the contained {@link ConditionalOnProperty @ConditionalOnProperty}
	 * annotations.
	 * @return the contained annotations
	 */
	ConditionalOnProperty[] value();

}
