/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;

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

	public PropertySources getPropertySources() {
		PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
		if (configurer != null) {
			return configurer.getAppliedPropertySources();
		}
		MutablePropertySources sources = extractEnvironmentPropertySources();
		if (sources != null) {
			return sources;
		}
		throw new IllegalStateException(
				"Unable to obtain PropertySources from " + "PropertySourcesPlaceholderConfigurer or Environment");
	}

	private MutablePropertySources extractEnvironmentPropertySources() {
		Environment environment = this.applicationContext.getEnvironment();
		if (environment instanceof ConfigurableEnvironment) {
			return ((ConfigurableEnvironment) environment).getPropertySources();
		}
		return null;
	}

	private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
		// Take care not to cause early instantiation of all FactoryBeans
		Map<String, PropertySourcesPlaceholderConfigurer> beans = this.applicationContext
				.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false, false);
		if (beans.size() == 1) {
			return beans.values().iterator().next();
		}
		if (beans.size() > 1 && logger.isWarnEnabled()) {
			logger.warn("Multiple PropertySourcesPlaceholderConfigurer " + "beans registered " + beans.keySet()
					+ ", falling back to Environment");
		}
		return null;
	}

}
