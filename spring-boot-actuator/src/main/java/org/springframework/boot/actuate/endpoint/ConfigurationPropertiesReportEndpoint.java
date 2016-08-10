/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint} to expose application properties from {@link ConfigurationProperties}
 * annotated beans.
 *
 * <p>
 * To protect sensitive information from being exposed, certain property values are masked
 * if their names end with a set of configurable values (default "password" and "secret").
 * Configure property names by using {@code endpoints.configprops.keys_to_sanitize} in
 * your Spring Boot application configuration.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "endpoints.configprops")
public class ConfigurationPropertiesReportEndpoint
		extends AbstractEndpoint<Map<String, Object>> implements ApplicationContextAware {

	private static final String CGLIB_FILTER_ID = "cglibFilter";

	private final Sanitizer sanitizer = new Sanitizer();

	private ApplicationContext context;

	public ConfigurationPropertiesReportEndpoint() {
		super("configprops");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	@Override
	public Map<String, Object> invoke() {
		return extract(this.context);
	}

	/**
	 * Extract beans annotated {@link ConfigurationProperties} and serialize into
	 * {@link Map}.
	 * @param context the application context
	 * @return the beans
	 */
	protected Map<String, Object> extract(ApplicationContext context) {
		// Serialize beans into map structure and sanitize values
		ObjectMapper mapper = new ObjectMapper();
		configureObjectMapper(mapper);
		return extract(context, mapper);
	}

	private Map<String, Object> extract(ApplicationContext context, ObjectMapper mapper) {
		Map<String, Object> result = new HashMap<String, Object>();
		ConfigurationBeanFactoryMetaData beanFactoryMetaData = getBeanFactoryMetaData(
				context);
		Map<String, Object> beans = getConfigurationPropertiesBeans(context,
				beanFactoryMetaData);
		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			String beanName = entry.getKey();
			Object bean = entry.getValue();
			Map<String, Object> root = new HashMap<String, Object>();
			String prefix = extractPrefix(context, beanFactoryMetaData, beanName, bean);
			root.put("prefix", prefix);
			root.put("properties", sanitize(prefix, safeSerialize(mapper, bean, prefix)));
			result.put(beanName, root);
		}
		if (context.getParent() != null) {
			result.put("parent", extract(context.getParent(), mapper));
		}
		return result;
	}

	private ConfigurationBeanFactoryMetaData getBeanFactoryMetaData(
			ApplicationContext context) {
		Map<String, ConfigurationBeanFactoryMetaData> beans = context
				.getBeansOfType(ConfigurationBeanFactoryMetaData.class);
		if (beans.size() == 1) {
			return beans.values().iterator().next();
		}
		return null;
	}

	private Map<String, Object> getConfigurationPropertiesBeans(
			ApplicationContext context,
			ConfigurationBeanFactoryMetaData beanFactoryMetaData) {
		Map<String, Object> beans = new HashMap<String, Object>();
		beans.putAll(context.getBeansWithAnnotation(ConfigurationProperties.class));
		if (beanFactoryMetaData != null) {
			beans.putAll(beanFactoryMetaData
					.getBeansWithFactoryAnnotation(ConfigurationProperties.class));
		}
		return beans;
	}

	/**
	 * Cautiously serialize the bean to a map (returning a map with an error message
	 * instead of throwing an exception if there is a problem).
	 * @param mapper the object mapper
	 * @param bean the source bean
	 * @param prefix the prefix
	 * @return the serialized instance
	 */
	private Map<String, Object> safeSerialize(ObjectMapper mapper, Object bean,
			String prefix) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> result = new HashMap<String, Object>(
					mapper.convertValue(bean, Map.class));
			return result;
		}
		catch (Exception ex) {
			return new HashMap<String, Object>(Collections.<String, Object>singletonMap(
					"error", "Cannot serialize '" + prefix + "'"));
		}
	}

	/**
	 * Configure Jackson's {@link ObjectMapper} to be used to serialize the
	 * {@link ConfigurationProperties} objects into a {@link Map} structure.
	 * @param mapper the object mapper
	 */
	protected void configureObjectMapper(ObjectMapper mapper) {
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		applyCglibFilters(mapper);
		applySerializationModifier(mapper);
	}

	/**
	 * Ensure only bindable and non-cyclic bean properties are reported.
	 * @param mapper the object mapper
	 */
	private void applySerializationModifier(ObjectMapper mapper) {
		SerializerFactory factory = BeanSerializerFactory.instance
				.withSerializerModifier(new GenericSerializerModifier());
		mapper.setSerializerFactory(factory);
	}

	/**
	 * Configure PropertyFilter to make sure Jackson doesn't process CGLIB generated bean
	 * properties.
	 * @param mapper the object mapper
	 */
	private void applyCglibFilters(ObjectMapper mapper) {
		mapper.setAnnotationIntrospector(new CglibAnnotationIntrospector());
		mapper.setFilterProvider(new SimpleFilterProvider().addFilter(CGLIB_FILTER_ID,
				new CglibBeanPropertyFilter()));
	}

	/**
	 * Extract configuration prefix from {@link ConfigurationProperties} annotation.
	 * @param context the application context
	 * @param beanFactoryMetaData the bean factory meta-data
	 * @param beanName the bean name
	 * @param bean the bean
	 * @return the prefix
	 */
	private String extractPrefix(ApplicationContext context,
			ConfigurationBeanFactoryMetaData beanFactoryMetaData, String beanName,
			Object bean) {
		ConfigurationProperties annotation = context.findAnnotationOnBean(beanName,
				ConfigurationProperties.class);
		if (beanFactoryMetaData != null) {
			ConfigurationProperties override = beanFactoryMetaData
					.findFactoryAnnotation(beanName, ConfigurationProperties.class);
			if (override != null) {
				// The @Bean-level @ConfigurationProperties overrides the one at type
				// level when binding. Arguably we should render them both, but this one
				// might be the most relevant for a starting point.
				annotation = override;
			}
		}
		return annotation.prefix();
	}

	/**
	 * Sanitize all unwanted configuration properties to avoid leaking of sensitive
	 * information.
	 * @param prefix the property prefix
	 * @param map the source map
	 * @return the sanitized map
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> sanitize(String prefix, Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			String qualifiedKey = (prefix.length() == 0 ? prefix : prefix + ".") + key;
			Object value = entry.getValue();
			if (value instanceof Map) {
				map.put(key, sanitize(qualifiedKey, (Map<String, Object>) value));
			}
			else {
				value = this.sanitizer.sanitize(key, value);
				value = this.sanitizer.sanitize(qualifiedKey, value);
				map.put(key, value);
			}
		}
		return map;
	}

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	@SuppressWarnings("serial")
	private static class CglibAnnotationIntrospector
			extends JacksonAnnotationIntrospector {

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

	/**
	 * {@link BeanSerializerModifier} to return only relevant configuration properties.
	 */
	protected static class GenericSerializerModifier extends BeanSerializerModifier {

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
				BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
			List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>();
			for (BeanPropertyWriter writer : beanProperties) {
				boolean readable = isReadable(beanDesc, writer);
				if (readable) {
					result.add(writer);
				}
			}
			return result;
		}

		private boolean isReadable(BeanDescription beanDesc, BeanPropertyWriter writer) {
			Class<?> parentType = beanDesc.getType().getRawClass();
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = findSetter(beanDesc, writer);
			// If there's a setter, we assume it's OK to report on the value,
			// similarly, if there's no setter but the package names match, we assume
			// that its a nested class used solely for binding to config props, so it
			// should be kosher. This filter is not used if there is JSON metadata for
			// the property, so it's mainly for user-defined beans.
			return (setter != null) || ClassUtils.getPackageName(parentType)
					.equals(ClassUtils.getPackageName(type));
		}

		private AnnotatedMethod findSetter(BeanDescription beanDesc,
				BeanPropertyWriter writer) {
			String name = "set" + StringUtils.capitalize(writer.getName());
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = beanDesc.findMethod(name, new Class<?>[] { type });
			// The enabled property of endpoints returns a boolean primitive but is set
			// using a Boolean class
			if (setter == null && type.equals(Boolean.TYPE)) {
				setter = beanDesc.findMethod(name, new Class<?>[] { Boolean.class });
			}
			return setter;
		}
	}

}
