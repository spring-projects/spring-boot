/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import java.util.function.Supplier;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a standard,
 * non-web environment {@link ConfigurableApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationContextRunner extends
		AbstractApplicationContextRunner<ApplicationContextRunner, ConfigurableApplicationContext, AssertableApplicationContext> {

	/**
	 * Create a new {@link ApplicationContextRunner} instance using an
	 * {@link AnnotationConfigApplicationContext} as the underlying source.
	 */
	public ApplicationContextRunner() {
		this(AnnotationConfigApplicationContext::new);
	}

	/**
	 * Create a new {@link ApplicationContextRunner} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 * @param contextFactory a supplier that returns a new instance on each call be added
	 * to the application context proxy
	 */
	public ApplicationContextRunner(Supplier<ConfigurableApplicationContext> contextFactory) {
		super(ApplicationContextRunner::new, contextFactory);
	}

	/**
	 * Create a new {@link ApplicationContextRunner} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 * @param contextFactory a supplier that returns a new instance on each call
	 * @param additionalContextInterfaces any additional application context interfaces to
	 * be added to the application context proxy
	 * @since 3.4.0
	 */
	public ApplicationContextRunner(Supplier<ConfigurableApplicationContext> contextFactory,
			Class<?>... additionalContextInterfaces) {
		super(ApplicationContextRunner::new, contextFactory, additionalContextInterfaces);
	}

	private ApplicationContextRunner(RunnerConfiguration<ConfigurableApplicationContext> runnerConfiguration) {
		super(runnerConfiguration, ApplicationContextRunner::new);
	}

}
