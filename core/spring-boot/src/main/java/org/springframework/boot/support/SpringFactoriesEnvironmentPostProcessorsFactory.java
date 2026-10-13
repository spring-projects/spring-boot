/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.support;

import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
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
		return this.loader.load(EnvironmentPostProcessor.class, argumentResolver);
	}

}
