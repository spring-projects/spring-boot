/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive.context;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link ApplicationContextFactory} registered in {@code spring.factories} to support
 * {@link AnnotationConfigReactiveWebServerApplicationContext} and
 * {@link ReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ReactiveWebServerApplicationContextFactory implements ApplicationContextFactory {

	@Override
	public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(
			@Nullable WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.REACTIVE) ? null : ApplicationReactiveWebEnvironment.class;
	}

	@Override
	public @Nullable ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.REACTIVE) ? null : new ApplicationReactiveWebEnvironment();
	}

	@Override
	public @Nullable ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.REACTIVE) ? null : createContext();
	}

	private ConfigurableApplicationContext createContext() {
		if (!AotDetector.useGeneratedArtifacts()) {
			return new AnnotationConfigReactiveWebServerApplicationContext();
		}
		return new ReactiveWebServerApplicationContext();
	}

}
