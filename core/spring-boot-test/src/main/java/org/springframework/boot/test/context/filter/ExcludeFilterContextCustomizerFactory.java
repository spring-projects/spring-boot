/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * {@link ContextCustomizerFactory} to add the {@link TestTypeExcludeFilter} to the
 * {@link ApplicationContext}.
 *
 * @author Phillip Webb
 * @see ExcludeFilterContextCustomizer
 */
class ExcludeFilterContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new ExcludeFilterContextCustomizer();
	}

}
