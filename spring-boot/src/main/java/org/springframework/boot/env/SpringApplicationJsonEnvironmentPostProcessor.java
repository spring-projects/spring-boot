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

package org.springframework.boot.env;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * An {@link EnvironmentPostProcessor} that parses JSON from
 * <code>spring.application.json</code> or equivalently
 * {@link SpringApplicationJsonEnvironmentPostProcessor} and adds it as a map property
 * source to the {@link Environment}. The new properties are added with higher priority
 * than the system properties.
 *
 * @author Dave Syer
 */
public class SpringApplicationJsonEnvironmentPostProcessor
		implements EnvironmentPostProcessor, Ordered {

	private static final Log logger = LogFactory
			.getLog(SpringApplicationJsonEnvironmentPostProcessor.class);

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

	private int order = DEFAULT_ORDER;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		String json = environment.resolvePlaceholders(
				"${spring.application.json:${SPRING_APPLICATION_JSON:}}");
		if (StringUtils.hasText(json)) {
			try {
				JsonParser parser = JsonParserFactory.getJsonParser();
				Map<String, Object> map = parser.parseMap(json);
				if (!map.isEmpty()) {
					MapPropertySource source = new MapPropertySource(
							"spring.application.json", flatten(map));
					MutablePropertySources sources = environment.getPropertySources();
					String name = findPropertySource(sources);
					if (sources.contains(name)) {
						sources.addBefore(name, source);
					}
					else {
						sources.addFirst(source);
					}
				}
			}
			catch (Exception e) {
				logger.warn("Cannot parse JSON for spring.application.json: " + json, e);
			}
		}
	}

	/**
	 * Flatten the map keys using period separator.
	 * @param map The map that should be flattened
	 * @return the flattened map
	 */
	private Map<String, Object> flatten(Map<String, Object> map) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		flatten(null, result, map);
		return result;
	}

	private void flatten(String prefix, Map<String, Object> result,
			Map<String, Object> map) {
		if (prefix == null) {
			prefix = "";
		}
		else {
			prefix = prefix + ".";
		}
		for (String key : map.keySet()) {
			String name = prefix + key;
			Object value = map.get(key);
			extract(name, result, value);
		}
	}

	private void extract(String name, Map<String, Object> result, Object value) {
		if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> nested = (Map<String, Object>) value;
			flatten(name, result, nested);
		}
		if (value instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<Object> nested = (Collection<Object>) value;
			int index = 0;
			for (Object object : nested) {
				extract(name + "[" + index + "]", result, object);
				index++;
			}
		}
		else {
			result.put(name, value);
		}
	}

	private String findPropertySource(MutablePropertySources sources) {
		if (ClassUtils.isPresent(
				"org.springframework.web.context.support.StandardServletEnvironment",
				null)
				&& sources
						.contains(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME)) {
			return StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME;

		}
		return StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;
	}

}
