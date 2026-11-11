/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;

/**
 * Utility to deduce the {@link PropertySources} to use for configuration binding.
 *
 * @author Phillip Webb
 */
class PropertySourcesDeducer {

	private static final Log logger = LogFactory.getLog(PropertySourcesDeducer.class);

	private final ApplicationContext applicationContext;

	PropertySourcesDeducer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	PropertySources getPropertySources() {
		PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
		if (configurer != null) {
			return configurer.getAppliedPropertySources();
		}
		MutablePropertySources sources = extractEnvironmentPropertySources();
		Assert.state(sources != null,
				"Unable to obtain PropertySources from PropertySourcesPlaceholderConfigurer or Environment");
		return sources;
	}

	private @Nullable PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
		// Take care not to cause early instantiation of all FactoryBeans
		Map<String, PropertySourcesPlaceholderConfigurer> beans = this.applicationContext
			.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false, false);
		if (beans.size() == 1) {
			return beans.values().iterator().next();
		}
		if (beans.size() > 1 && logger.isWarnEnabled()) {
			logger.warn("Multiple PropertySourcesPlaceholderConfigurer beans registered " + beans.keySet()
					+ ", falling back to Environment");
		}
		return null;
	}

	private @Nullable MutablePropertySources extractEnvironmentPropertySources() {
		Environment environment = this.applicationContext.getEnvironment();
		if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
			return configurableEnvironment.getPropertySources();
		}
		return null;
	}

}
