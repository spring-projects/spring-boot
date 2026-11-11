/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * {@link ContextCustomizerFactory} to map annotation attributes to {@link Environment}
 * properties.
 *
 * @author Phillip Webb
 */
class PropertyMappingContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configurationAttributes) {
		AnnotationsPropertySource propertySource = new AnnotationsPropertySource(testClass);
		return new PropertyMappingContextCustomizer(propertySource);
	}

}
