/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.context.servlet.ApplicationServletEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link ApplicationContextFactory} registered in {@code spring.factories} to support
 * {@link AnnotationConfigServletWebServerApplicationContext} and
 * {@link ServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ServletWebServerApplicationContextFactory implements ApplicationContextFactory {

	@Override
	public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(
			@Nullable WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.SERVLET) ? null : ApplicationServletEnvironment.class;
	}

	@Override
	public @Nullable ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.SERVLET) ? null : new ApplicationServletEnvironment();
	}

	@Override
	public @Nullable ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.SERVLET) ? null : createContext();
	}

	private ConfigurableApplicationContext createContext() {
		if (!AotDetector.useGeneratedArtifacts()) {
			return new AnnotationConfigServletWebServerApplicationContext();
		}
		return new ServletWebServerApplicationContext();
	}

}
