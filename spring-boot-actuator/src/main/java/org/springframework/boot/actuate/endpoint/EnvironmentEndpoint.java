/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Endpoint} to expose {@link ConfigurableEnvironment environment} information.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 */
@ConfigurationProperties(prefix = "endpoints.env", ignoreUnknownFields = false)
public class EnvironmentEndpoint extends AbstractEndpoint<Map<String, Object>> implements
		EnvironmentAware {

	private Environment environment;

	private String[] keysToSanitize = new String[] { "password", "secret", "key" };

	/**
	 * Create a new {@link EnvironmentEndpoint} instance.
	 */
	public EnvironmentEndpoint() {
		super("env");
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
		this.keysToSanitize = keysToSanitize;
	}

	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("profiles", this.environment.getActiveProfiles());
		for (Entry<String, PropertySource<?>> entry : getPropertySources().entrySet()) {
			PropertySource<?> source = entry.getValue();
			String sourceName = entry.getKey();
			if (source instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				for (String name : enumerable.getPropertyNames()) {
					map.put(name, sanitize(name, enumerable.getProperty(name)));
				}
				result.put(sourceName, map);
			}
		}
		return result;
	}

	private Map<String, PropertySource<?>> getPropertySources() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<String, PropertySource<?>>();
		MutablePropertySources sources = null;
		if (this.environment != null
				&& this.environment instanceof ConfigurableEnvironment) {
			sources = ((ConfigurableEnvironment) this.environment).getPropertySources();
		}
		else {
			sources = new StandardEnvironment().getPropertySources();
		}
		for (PropertySource<?> source : sources) {
			extract("", map, source);
		}
		return map;
	}

	private void extract(String root, Map<String, PropertySource<?>> map,
			PropertySource<?> source) {
		if (source instanceof CompositePropertySource) {
			try {
				Field field = ReflectionUtils.findField(CompositePropertySource.class,
						"propertySources");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				Set<PropertySource<?>> nested = (Set<PropertySource<?>>) field
						.get(source);
				for (PropertySource<?> nest : nested) {
					extract(source.getName() + ":", map, nest);
				}
			}
			catch (Exception e) {
				// ignore
			}
		}
		else {
			map.put(root + source.getName(), source);
		}
	}

	public Object sanitize(String name, Object object) {
		for (String keyToSanitize : this.keysToSanitize) {
			if (name.toLowerCase().endsWith(keyToSanitize)) {
				return (object == null ? null : "******");
			}
		}
		return object;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
