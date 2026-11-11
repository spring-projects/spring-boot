/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Callback interface used to process an {@link ApplicationContext} with the ability to
 * throw a (checked) exception.
 *
 * @param <C> the application context type
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see AbstractApplicationContextRunner
 */
@FunctionalInterface
public interface ContextConsumer<C extends ApplicationContext> {

	/**
	 * Performs this operation on the supplied {@code context}.
	 * @param context the application context to consume
	 * @throws Throwable any exception that might occur in assertions
	 */
	void accept(C context) throws Throwable;

	/**
	 * Returns a composed {@code ContextConsumer} that performs, in sequence, this
	 * operation followed by the {@code after} operation.
	 * @param after the operation to perform after this operation
	 * @return a composed {@code ContextConsumer} that performs in sequence this operation
	 * followed by the {@code after} operation
	 * @since 2.6.0
	 */
	default ContextConsumer<C> andThen(ContextConsumer<? super C> after) {
		Assert.notNull(after, "'after' must not be null");
		return (context) -> {
			accept(context);
			after.accept(context);
		};
	}

}
