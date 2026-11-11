/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.assertj;

import java.util.function.Supplier;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * An {@link ApplicationContext} that additionally supports AssertJ style assertions. Can
 * be used to decorate an existing application context or an application context that
 * failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see ApplicationContextRunner
 * @see ApplicationContext
 */
public interface AssertableApplicationContext
		extends ApplicationContextAssertProvider<AssertableApplicationContext, ConfigurableApplicationContext>,
		ConfigurableApplicationContext {

	/**
	 * Factory method to create a new {@link AssertableApplicationContext} instance.
	 * @param contextSupplier a supplier that will either return a fully configured
	 * {@link ConfigurableApplicationContext} or throw an exception if the context fails
	 * to start.
	 * @return an {@link AssertableApplicationContext} instance
	 */
	static AssertableApplicationContext get(Supplier<? extends ConfigurableApplicationContext> contextSupplier) {
		return ApplicationContextAssertProvider.get(AssertableApplicationContext.class,
				ConfigurableApplicationContext.class, contextSupplier);
	}

	/**
	 * Factory method to create a new {@link AssertableApplicationContext} instance.
	 * @param contextSupplier a supplier that will either return a fully configured
	 * {@link ConfigurableApplicationContext} or throw an exception if the context fails
	 * to start.
	 * @param additionalContextInterfaces and additional context interfaces to add to the
	 * proxy
	 * @return an {@link AssertableApplicationContext} instance
	 * @since 3.4.0
	 */
	static AssertableApplicationContext get(Supplier<? extends ConfigurableApplicationContext> contextSupplier,
			Class<?>... additionalContextInterfaces) {
		return ApplicationContextAssertProvider.get(AssertableApplicationContext.class,
				ConfigurableApplicationContext.class, contextSupplier, additionalContextInterfaces);
	}

}
