/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link Endpoint} to expose {@link ConfigurableEnvironment environment} information.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@ConfigurationProperties(name = "endpoints.env", ignoreUnknownFields = false)
public class EnvironmentEndpoint extends AbstractEndpoint<Map<String, Object>> implements
		EnvironmentAware {

	private Environment environment;

	/**
	 * Create a new {@link EnvironmentEndpoint} instance.
	 */
	public EnvironmentEndpoint() {
		super("/env");
	}

	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("profiles", this.environment.getActiveProfiles());
		for (PropertySource<?> source : getPropertySources()) {
			if (source instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				for (String name : enumerable.getPropertyNames()) {
					map.put(name, sanitize(name, enumerable.getProperty(name)));
				}
				result.put(source.getName(), map);
			}
		}
		return result;
	}

	private Iterable<PropertySource<?>> getPropertySources() {
		if (this.environment != null
				&& this.environment instanceof ConfigurableEnvironment) {
			return ((ConfigurableEnvironment) this.environment).getPropertySources();
		}
		return new StandardEnvironment().getPropertySources();
	}

	private Object sanitize(String name, Object object) {
		if (name.toLowerCase().endsWith("password")
				|| name.toLowerCase().endsWith("secret")) {
			return object == null ? null : "******";
		}
		return object;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
