/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks if the given
 * {@link ForwardedHeaderStrategy} is selected by the user.
 *
 * @author Phillip Webb
 * @since 4.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Documented
@Conditional(OnForwardedHeaderStrategyCondition.class)
public @interface ConditionalOnForwardedHeaderStrategy {

	/**
	 * The required strategy.
	 * @return the strategy
	 */
	ForwardedHeaderStrategy value();

}
