/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.lang.Contract;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a Servlet
 * based {@link ConfigurableWebApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class WebApplicationContextRunner extends
		AbstractApplicationContextRunner<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

	/**
	 * Create a new {@link WebApplicationContextRunner} instance using an
	 * {@link AnnotationConfigServletWebApplicationContext} with a
	 * {@link MockServletContext} as the underlying source.
	 * @see #withMockServletContext(Supplier)
	 */
	public WebApplicationContextRunner() {
		this(withMockServletContext(AnnotationConfigServletWebApplicationContext::new));
	}

	/**
	 * Create a new {@link WebApplicationContextRunner} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 * @param contextFactory a supplier that returns a new instance on each call be added
	 * to the application context proxy
	 */
	public WebApplicationContextRunner(Supplier<ConfigurableWebApplicationContext> contextFactory) {
		super(WebApplicationContextRunner::new, contextFactory);
	}

	/**
	 * Create a new {@link WebApplicationContextRunner} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 * @param contextFactory a supplier that returns a new instance on each call
	 * @param additionalContextInterfaces any additional application context interfaces to
	 * be added to the application context proxy
	 * @since 3.4.0
	 */
	public WebApplicationContextRunner(Supplier<ConfigurableWebApplicationContext> contextFactory,
			Class<?>... additionalContextInterfaces) {
		super(WebApplicationContextRunner::new, contextFactory, additionalContextInterfaces);
	}

	private WebApplicationContextRunner(RunnerConfiguration<ConfigurableWebApplicationContext> configuration) {
		super(configuration, WebApplicationContextRunner::new);
	}

	/**
	 * Decorate the specified {@code contextFactory} to set a {@link MockServletContext}
	 * on each newly created {@link WebApplicationContext}.
	 * @param contextFactory the context factory to decorate
	 * @return an updated supplier that will set the {@link MockServletContext}
	 */
	@Contract("!null -> !null")
	public static @Nullable Supplier<ConfigurableWebApplicationContext> withMockServletContext(
			@Nullable Supplier<ConfigurableWebApplicationContext> contextFactory) {
		return (contextFactory != null) ? () -> {
			ConfigurableWebApplicationContext context = contextFactory.get();
			context.setServletContext(new MockServletContext());
			return context;
		} : null;
	}

}
