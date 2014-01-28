/*
 * Copyright 2013 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link Endpoint} to expose application properties from {@link ConfigurationProperties}
 * annotated classes.
 * 
 * <p>
 * To protect sensitive information from being exposed, certain property values are masked
 * if their names end with a set of configurable values (default "password" and "secret").
 * Configure property names by using {@link #setKeysToSanitize(String[])}.
 * 
 * @author Christian Dupuis
 */
@ConfigurationProperties(name = "endpoints.configprops", ignoreUnknownFields = false)
public class ConfigurationPropertiesReportEndpoint extends
		AbstractEndpoint<Map<String, Object>> implements ApplicationContextAware {

	private String[] keysToSanitize = new String[] { "password", "secret" };

	private ApplicationContext context;

	public ConfigurationPropertiesReportEndpoint() {
		super("configprops");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public String[] getKeysToSanitize() {
		return this.keysToSanitize;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
		this.keysToSanitize = keysToSanitize;
	}

	@Override
	public Map<String, Object> invoke() {
		return extract(this.context);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extract(ApplicationContext context) {

		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> beans = context
				.getBeansWithAnnotation(ConfigurationProperties.class);

		// Serialize beans into map structure and sanitize values
		ObjectMapper mapper = new ObjectMapper();
		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			String beanName = entry.getKey();
			Object bean = entry.getValue();

			Map<String, Object> root = new HashMap<String, Object>();
			root.put("prefix", extractPrefix(bean));
			root.put("properties", sanitize(mapper.convertValue(bean, Map.class)));
			result.put(beanName, root);
		}

		if (context.getParent() != null) {
			result.put("parent", extract(context.getParent()));
		}

		return result;
	}

	private String extractPrefix(Object bean) {
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
				bean.getClass(), ConfigurationProperties.class);
		return (StringUtils.hasLength(annotation.value()) ? annotation.value()
				: annotation.name());
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> sanitize(Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() instanceof Map) {
				map.put(entry.getKey(), sanitize((Map<String, Object>) entry.getValue()));
			}
			else {
				map.put(entry.getKey(), sanitize(entry.getKey(), entry.getValue()));
			}
		}
		return map;
	}

	private Object sanitize(String name, Object object) {
		for (String keyToSanitize : this.keysToSanitize) {
			if (name.toLowerCase().endsWith(keyToSanitize)) {
				return (object == null ? null : "******");
			}
		}
		return object;
	}

}
