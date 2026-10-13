/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.io;

import java.util.List;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * {@link ApplicationContextInitializer} that adds all {@link ProtocolResolver
 * ProtocolResolvers} registered in a {@code spring.factories} file.
 *
 * @author Scott Frederick
 */
class ProtocolResolverApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		SpringFactoriesLoader loader = SpringFactoriesLoader
			.forDefaultResourceLocation(applicationContext.getClassLoader());
		List<ProtocolResolver> protocolResolvers = loader.load(ProtocolResolver.class);
		protocolResolvers.forEach(applicationContext::addProtocolResolver);
	}

}
