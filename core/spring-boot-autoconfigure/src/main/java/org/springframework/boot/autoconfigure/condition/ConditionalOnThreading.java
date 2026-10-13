/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when the specified threading is active.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnThreadingCondition.class)
public @interface ConditionalOnThreading {

	/**
	 * The {@link Threading threading} that must be active.
	 * @return the expected threading
	 */
	Threading value();

}
