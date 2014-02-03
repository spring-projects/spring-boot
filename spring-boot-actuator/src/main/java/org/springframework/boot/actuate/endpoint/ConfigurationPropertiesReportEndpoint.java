/*
 * Copyright 2013-2014 the original author or authors.
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * {@link Endpoint} to expose application properties from {@link ConfigurationProperties}
 * annotated beans.
 * 
 * <p>
 * To protect sensitive information from being exposed, certain property values are masked
 * if their names end with a set of configurable values (default "password" and "secret").
 * Configure property names by using <code>endpoints.configprops.keys_to_sanitize</code>
 * in your Spring Boot application configuration.
 * 
 * @author Christian Dupuis
 */
@ConfigurationProperties(name = "endpoints.configprops", ignoreUnknownFields = false)
public class ConfigurationPropertiesReportEndpoint extends
		AbstractEndpoint<Map<String, Object>> implements ApplicationContextAware {

	private static final String CGLIB_FILTER_ID = "cglibFilter";

	private String[] keysToSanitize = new String[] { "password", "secret" };

	private ApplicationContext context;

	public ConfigurationPropertiesReportEndpoint() {
		super("configprops");
	}

	public String[] getKeysToSanitize() {
		return this.keysToSanitize;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
		this.keysToSanitize = keysToSanitize;
	}

	@Override
	public Map<String, Object> invoke() {
		return extract(this.context);
	}

	/**
	 * Extract beans annotated {@link ConfigurationProperties} and serialize into
	 * {@link Map}.
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> extract(ApplicationContext context) {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> beans = context
				.getBeansWithAnnotation(ConfigurationProperties.class);

		// Serialize beans into map structure and sanitize values
		ObjectMapper mapper = new ObjectMapper();
		configureObjectMapper(mapper);

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

	/**
	 * Configure Jackson's {@link ObjectMapper} to be used to serialize the
	 * {@link ConfigurationProperties} objects into a {@link Map} structure.
	 */
	protected void configureObjectMapper(ObjectMapper mapper) {
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		applyCglibFilters(mapper);
	}

	/**
	 * Configure PropertyFiler to make sure Jackson doesn't process CGLIB generated bean
	 * properties.
	 */
	private void applyCglibFilters(ObjectMapper mapper) {
		mapper.setAnnotationIntrospector(new CglibAnnotationIntrospector());
		mapper.setFilters(new SimpleFilterProvider().addFilter(CGLIB_FILTER_ID,
				new CglibBeanPropertyFilter()));
	}

	/**
	 * Extract configuration prefix from {@link ConfigurationProperties} annotation.
	 */
	private String extractPrefix(Object bean) {
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
				bean.getClass(), ConfigurationProperties.class);
		return (StringUtils.hasLength(annotation.value()) ? annotation.value()
				: annotation.name());
	}

	/**
	 * Sanitize all unwanted configuration properties to avoid leaking of sensitive
	 * information.
	 */
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

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	private static class CglibAnnotationIntrospector extends
			JacksonAnnotationIntrospector {

		@Override
		public Object findFilterId(Annotated a) {
			Object id = super.findFilterId(a);
			if (id == null) {
				id = CGLIB_FILTER_ID;
			}
			return id;
		}

	}

	/**
	 * {@link SimpleBeanPropertyFilter} to filter out all bean properties whose names
	 * start with '$$'.
	 */
	private static class CglibBeanPropertyFilter extends SimpleBeanPropertyFilter {

		@Override
		protected boolean include(BeanPropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		@Override
		protected boolean include(PropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		private boolean include(String name) {
			return !name.startsWith("$$");
		}

	}

}
