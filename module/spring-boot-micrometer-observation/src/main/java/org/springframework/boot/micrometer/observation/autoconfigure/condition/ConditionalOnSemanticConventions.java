/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when the specified semantic conventions
 * are enabled.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnSemanticConventionsCondition.class)
public @interface ConditionalOnSemanticConventions {

	/**
	 * The semantic conventions that must be enabled for the condition to match.
	 * @return the conventions
	 */
	SemanticConventions value();

}
