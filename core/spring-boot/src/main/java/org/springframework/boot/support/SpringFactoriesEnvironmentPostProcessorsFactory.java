/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.support;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;

/**
 * An {@link EnvironmentPostProcessorsFactory} that uses {@link SpringFactoriesLoader}.
 *
 * @author Andy Wilkinson
 */
class SpringFactoriesEnvironmentPostProcessorsFactory implements EnvironmentPostProcessorsFactory {

	private final SpringFactoriesLoader loader;

	SpringFactoriesEnvironmentPostProcessorsFactory(SpringFactoriesLoader loader) {
		this.loader = loader;
	}

	@Override
	public List<EnvironmentPostProcessor> getEnvironmentPostProcessors(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext) {
		ArgumentResolver argumentResolver = ArgumentResolver.of(DeferredLogFactory.class, logFactory);
		argumentResolver = argumentResolver.and(ConfigurableBootstrapContext.class, bootstrapContext);
		argumentResolver = argumentResolver.and(BootstrapContext.class, bootstrapContext);
		argumentResolver = argumentResolver.and(BootstrapRegistry.class, bootstrapContext);
		List<Object> postProcessors = new ArrayList<>();
		postProcessors.addAll(this.loader.load(EnvironmentPostProcessor.class, argumentResolver));
		postProcessors.addAll(loadDeprecatedPostProcessors(argumentResolver));
		AnnotationAwareOrderComparator.sort(postProcessors);
		return postProcessors.stream().map(Adapter::apply).collect(Collectors.toCollection(ArrayList::new));
	}

	@SuppressWarnings("removal")
	private List<org.springframework.boot.env.EnvironmentPostProcessor> loadDeprecatedPostProcessors(
			ArgumentResolver argumentResolver) {
		return this.loader.load(org.springframework.boot.env.EnvironmentPostProcessor.class, argumentResolver);
	}

	@SuppressWarnings("removal")
	record Adapter(
			org.springframework.boot.env.EnvironmentPostProcessor postProcessor) implements EnvironmentPostProcessor {

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
			this.postProcessor.postProcessEnvironment(environment, application);
		}

		static EnvironmentPostProcessor apply(Object source) {
			if (source instanceof EnvironmentPostProcessor environmentPostProcessor) {
				return environmentPostProcessor;
			}
			return new Adapter((org.springframework.boot.env.EnvironmentPostProcessor) source);
		}

	}

}
