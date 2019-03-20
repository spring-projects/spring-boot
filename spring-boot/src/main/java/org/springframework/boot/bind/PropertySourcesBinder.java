/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

/**
 * Helper extracting info from {@link PropertySources}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class PropertySourcesBinder {

	private PropertySources propertySources;

	private ConversionService conversionService;

	/**
	 * Create a new instance.
	 * @param propertySources the {@link PropertySources} to use
	 */
	public PropertySourcesBinder(PropertySources propertySources) {
		this.propertySources = propertySources;
	}

	/**
	 * Create a new instance from a single {@link PropertySource}.
	 * @param propertySource the {@link PropertySource} to use
	 */
	public PropertySourcesBinder(PropertySource<?> propertySource) {
		this(createPropertySources(propertySource));
	}

	/**
	 * Create a new instance using the {@link Environment} as the property sources.
	 * @param environment the environment
	 */
	public PropertySourcesBinder(ConfigurableEnvironment environment) {
		this(environment.getPropertySources());
	}

	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = propertySources;
	}

	public PropertySources getPropertySources() {
		return this.propertySources;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Extract the keys using the specified {@code prefix}. The prefix won't be included.
	 * <p>
	 * Any key that starts with the {@code prefix} will be included.
	 * @param prefix the prefix to use
	 * @return the keys matching the prefix
	 */
	public Map<String, Object> extractAll(String prefix) {
		Map<String, Object> content = new LinkedHashMap<String, Object>();
		bindTo(prefix, content);
		return content;
	}

	/**
	 * Bind the specified {@code target} from the environment using the {@code prefix}.
	 * <p>
	 * Any key that starts with the {@code prefix} will be bound to the {@code target}.
	 * @param prefix the prefix to use
	 * @param target the object to bind to
	 */
	public void bindTo(String prefix, Object target) {
		PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
				target);
		if (StringUtils.hasText(prefix)) {
			factory.setTargetName(prefix);
		}
		if (this.conversionService != null) {
			factory.setConversionService(this.conversionService);
		}
		factory.setPropertySources(this.propertySources);
		try {
			factory.bindPropertiesToTarget();
		}
		catch (BindException ex) {
			throw new IllegalStateException("Cannot bind to " + target, ex);
		}
	}

	private static PropertySources createPropertySources(
			PropertySource<?> propertySource) {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(propertySource);
		return propertySources;
	}

}
