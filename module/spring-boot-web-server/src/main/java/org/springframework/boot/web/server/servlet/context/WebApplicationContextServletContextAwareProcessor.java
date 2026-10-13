/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

/**
 * Variant of {@link ServletContextAwareProcessor} for use with a
 * {@link ConfigurableWebApplicationContext}. Can be used when registering the processor
 * can occur before the {@link ServletContext} or {@link ServletConfig} have been
 * initialized.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class WebApplicationContextServletContextAwareProcessor extends ServletContextAwareProcessor {

	private final ConfigurableWebApplicationContext webApplicationContext;

	public WebApplicationContextServletContextAwareProcessor(ConfigurableWebApplicationContext webApplicationContext) {
		Assert.notNull(webApplicationContext, "'webApplicationContext' must not be null");
		this.webApplicationContext = webApplicationContext;
	}

	@Override
	protected @Nullable ServletContext getServletContext() {
		ServletContext servletContext = this.webApplicationContext.getServletContext();
		return (servletContext != null) ? servletContext : super.getServletContext();
	}

	@Override
	protected @Nullable ServletConfig getServletConfig() {
		ServletConfig servletConfig = this.webApplicationContext.getServletConfig();
		return (servletConfig != null) ? servletConfig : super.getServletConfig();
	}

}
