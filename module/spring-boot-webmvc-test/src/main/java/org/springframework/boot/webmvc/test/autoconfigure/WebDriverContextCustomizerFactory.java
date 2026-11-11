/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import java.util.List;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * {@link ContextCustomizerFactory} to register a {@link WebDriverScope} and configure
 * appropriate bean definitions to use it. Expects the scope to be reset with a
 * {@link WebDriverTestExecutionListener}.
 *
 * @author Phillip Webb
 * @see WebDriverTestExecutionListener
 * @see WebDriverScope
 */
class WebDriverContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new WebDriverContextCustomizer();
	}

}
