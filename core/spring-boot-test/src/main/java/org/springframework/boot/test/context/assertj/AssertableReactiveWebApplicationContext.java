/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.assertj;

import java.util.function.Supplier;

import org.springframework.boot.web.context.reactive.ConfigurableReactiveWebApplicationContext;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;

/**
 * A {@link ReactiveWebApplicationContext} that additionally supports AssertJ style
 * assertions. Can be used to decorate an existing reactive web application context or an
 * application context that failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see ReactiveWebApplicationContext
 * @see ReactiveWebApplicationContext
 */
public interface AssertableReactiveWebApplicationContext extends
		ApplicationContextAssertProvider<AssertableReactiveWebApplicationContext, ConfigurableReactiveWebApplicationContext>,
		ConfigurableReactiveWebApplicationContext {

	/**
	 * Factory method to create a new {@link AssertableReactiveWebApplicationContext}
	 * instance.
	 * @param contextSupplier a supplier that will either return a fully configured
	 * {@link ConfigurableReactiveWebApplicationContext} or throw an exception if the
	 * context fails to start.
	 * @return a {@link AssertableReactiveWebApplicationContext} instance
	 */
	static AssertableReactiveWebApplicationContext get(
			Supplier<? extends ConfigurableReactiveWebApplicationContext> contextSupplier) {
		return ApplicationContextAssertProvider.get(AssertableReactiveWebApplicationContext.class,
				ConfigurableReactiveWebApplicationContext.class, contextSupplier);
	}

	/**
	 * Factory method to create a new {@link AssertableReactiveWebApplicationContext}
	 * instance.
	 * @param contextSupplier a supplier that will either return a fully configured
	 * {@link ConfigurableReactiveWebApplicationContext} or throw an exception if the
	 * context fails to start.
	 * @param additionalContextInterfaces and additional context interfaces to add to the
	 * proxy
	 * @return a {@link AssertableReactiveWebApplicationContext} instance
	 * @since 3.4.0
	 */
	static AssertableReactiveWebApplicationContext get(
			Supplier<? extends ConfigurableReactiveWebApplicationContext> contextSupplier,
			Class<?>... additionalContextInterfaces) {
		return ApplicationContextAssertProvider.get(AssertableReactiveWebApplicationContext.class,
				ConfigurableReactiveWebApplicationContext.class, contextSupplier, additionalContextInterfaces);
	}

}
