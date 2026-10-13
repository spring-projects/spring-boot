/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * {@link ContextCustomizerFactory} implementation to apply
 * {@link SpringBootTestRandomPortContextCustomizer} to tests.
 *
 * @author Phillip Webb
 */
class SpringBootTestRandomPortContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new SpringBootTestRandomPortContextCustomizer();
	}

}
