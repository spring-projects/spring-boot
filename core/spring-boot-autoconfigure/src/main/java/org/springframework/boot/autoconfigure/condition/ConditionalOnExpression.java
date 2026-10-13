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
 * Configuration annotation for a conditional element that depends on the value of a SpEL
 * expression.
 * <p>
 * Referencing a bean in the expression will cause that bean to be initialized very early
 * in context refresh processing. As a result, the bean won't be eligible for
 * post-processing (such as configuration properties binding) and its state may be
 * incomplete.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnExpressionCondition.class)
public @interface ConditionalOnExpression {

	/**
	 * The SpEL expression to evaluate. Expression should return {@code true} if the
	 * condition passes or {@code false} if it fails.
	 * @return the SpEL expression
	 */
	String value() default "true";

}
