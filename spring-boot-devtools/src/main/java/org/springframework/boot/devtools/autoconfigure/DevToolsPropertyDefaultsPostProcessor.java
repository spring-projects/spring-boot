/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * {@link BeanFactoryPostProcessor} to add properties that make sense when working at
 * development time.
 *
 * @author Phillip Webb
 */
class DevToolsPropertyDefaultsPostProcessor implements BeanFactoryPostProcessor,
		EnvironmentAware {

	private static final Map<String, Object> PROPERTIES;
	static {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.thymeleaf.cache", "false");
		properties.put("spring.freemarker.cache", "false");
		properties.put("spring.groovy.template.cache", "false");
		properties.put("spring.velocity.cache", "false");
		properties.put("spring.mustache.cache", "false");
		PROPERTIES = Collections.unmodifiableMap(properties);
	}

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		if (this.environment instanceof ConfigurableEnvironment) {
			postProcessEnvironment((ConfigurableEnvironment) this.environment);
		}
	}

	private void postProcessEnvironment(ConfigurableEnvironment environment) {
		PropertySource<?> propertySource = new MapPropertySource("refresh", PROPERTIES);
		environment.getPropertySources().addFirst(propertySource);
	}

}
