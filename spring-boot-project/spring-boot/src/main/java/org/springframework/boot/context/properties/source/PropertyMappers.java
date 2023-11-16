/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Adapter to find {@link ConfigurationPropertyName} in {@link PropertySource} then return
 * {@link PropertySource} name.
 *
 * @author Wang Zhiyang
 * @since 3.2
 */
public final class PropertyMappers {

	private PropertyMappers() {

	}

	public static String map(PropertySource<?> source, ConfigurationPropertyName name) {
		if (source == null || name == null) {
			return null;
		}
		for (PropertyMapper mapper : getPropertyMappers(source)) {
			try {
				for (String candidate : mapper.map(name)) {
					if (source.containsProperty(candidate)) {
						return candidate;
					}
				}
			}
			catch (Exception ex) {
			}
		}
		return null;
	}

	private static PropertyMapper[] getPropertyMappers(PropertySource<?> source) {
		if (source instanceof SystemEnvironmentPropertySource && hasSystemEnvironmentName(source)) {
			return new PropertyMapper[] { SystemEnvironmentPropertyMapper.INSTANCE, DefaultPropertyMapper.INSTANCE };
		}
		return new PropertyMapper[] { DefaultPropertyMapper.INSTANCE };
	}

	private static boolean hasSystemEnvironmentName(PropertySource<?> source) {
		String name = source.getName();
		return StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(name)
				|| name.endsWith("-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
	}

}
