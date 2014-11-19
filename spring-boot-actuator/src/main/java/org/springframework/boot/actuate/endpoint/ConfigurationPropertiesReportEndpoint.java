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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonMappingException;
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
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "endpoints.configprops", ignoreUnknownFields = false)
public class ConfigurationPropertiesReportEndpoint extends
		AbstractEndpoint<Map<String, Object>> implements ApplicationContextAware {

	private static final String CGLIB_FILTER_ID = "cglibFilter";

	private static final Log logger = LogFactory
			.getLog(ConfigurationPropertiesReportEndpoint.class);

	private final Sanitizer sanitizer = new Sanitizer();

	private ApplicationContext context;

	private ConfigurationBeanFactoryMetaData beanFactoryMetaData;

	private ConfigurationPropertiesMetaData metadata;

	private String metadataLocations = "classpath:*/META-INF/*spring-configuration-metadata.json";

	public ConfigurationPropertiesReportEndpoint() {
		super("configprops");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public void setConfigurationBeanFactoryMetaData(
			ConfigurationBeanFactoryMetaData beanFactoryMetaData) {
		this.beanFactoryMetaData = beanFactoryMetaData;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	/**
	 * Location path for JSON metadata about config properties.
	 *
	 * @param metadataLocations the metadataLocations to set
	 */
	public void setMetadataLocations(String metadataLocations) {
		this.metadataLocations = metadataLocations;
	}

	@Override
	public Map<String, Object> invoke() {
		return extract(this.context);
	}

	/**
	 * Extract beans annotated {@link ConfigurationProperties} and serialize into
	 * {@link Map}.
	 */
	protected Map<String, Object> extract(ApplicationContext context) {

		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> beans = new HashMap<String, Object>(
				context.getBeansWithAnnotation(ConfigurationProperties.class));
		if (this.beanFactoryMetaData != null) {
			beans.putAll(this.beanFactoryMetaData
					.getBeansWithFactoryAnnotation(ConfigurationProperties.class));
		}

		// Serialize beans into map structure and sanitize values
		ObjectMapper mapper = new ObjectMapper();
		configureObjectMapper(mapper);

		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			String beanName = entry.getKey();
			Object bean = entry.getValue();
			Map<String, Object> root = new HashMap<String, Object>();
			String prefix = extractPrefix(context, beanName, bean);
			root.put("prefix", prefix);
			root.put("properties", sanitize(safeSerialize(mapper, bean, prefix)));
			result.put(beanName, root);
		}

		if (context.getParent() != null) {
			result.put("parent", extract(context.getParent()));
		}

		return result;
	}

	/**
	 * Cautiously serialize the bean to a map (returning a map with an error message
	 * instead of throwing an exception if there is a problem).
	 */
	private Map<String, Object> safeSerialize(ObjectMapper mapper, Object bean,
			String prefix) {
		if (this.metadata == null) {
			this.metadata = new ConfigurationPropertiesMetaData(this.metadataLocations);
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> result = new HashMap<String, Object>(mapper.convertValue(
					this.metadata.extractMap(bean, prefix), Map.class));
			return result;
		}
		catch (Exception e) {
			return new HashMap<String, Object>(Collections.<String, Object> singletonMap(
					"error", "Cannot serialize '" + prefix + "'"));
		}
	}

	/**
	 * Configure Jackson's {@link ObjectMapper} to be used to serialize the
	 * {@link ConfigurationProperties} objects into a {@link Map} structure.
	 */
	protected void configureObjectMapper(ObjectMapper mapper) {
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		applyCglibFilters(mapper);
		applySerializationModifier(mapper);
	}

	/**
	 * Ensure only bindable and non-cyclic bean properties are reported.
	 */
	private void applySerializationModifier(ObjectMapper mapper) {
		SerializerFactory factory = BeanSerializerFactory.instance
				.withSerializerModifier(new GenericSerializerModifier());
		mapper.setSerializerFactory(factory);
	}

	/**
	 * Configure PropertyFilter to make sure Jackson doesn't process CGLIB generated bean
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
	private String extractPrefix(ApplicationContext context, String beanName, Object bean) {
		ConfigurationProperties annotation = context.findAnnotationOnBean(beanName,
				ConfigurationProperties.class);
		if (this.beanFactoryMetaData != null) {
			ConfigurationProperties override = this.beanFactoryMetaData
					.findFactoryAnnotation(beanName, ConfigurationProperties.class);
			if (override != null) {
				// The @Bean-level @ConfigurationProperties overrides the one at type
				// level when binding. Arguably we should render them both, but this one
				// might be the most relevant for a starting point.
				annotation = override;
			}
		}
		return (StringUtils.hasLength(annotation.value()) ? annotation.value()
				: annotation.prefix());
	}

	/**
	 * Sanitize all unwanted configuration properties to avoid leaking of sensitive
	 * information.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> sanitize(Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				map.put(key, sanitize((Map<String, Object>) value));
			}
			else {
				map.put(key, this.sanitizer.sanitize(key, value));
			}
		}
		return map;
	}

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	@SuppressWarnings("serial")
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
			String parenType = beanDesc.getType().getRawClass().getName();
			String type = writer.getPropertyType().getName();
			AnnotatedMethod setter = beanDesc.findMethod(
					"set" + StringUtils.capitalize(writer.getName()),
					new Class<?>[] { writer.getPropertyType() });
			// If there's a setter, we assume it's OK to report on the value,
			// similarly, if there's no setter but the package names match, we assume
			// that its a nested class used solely for binding to config props, so it
			// should be kosher. This filter is not used if there is JSON metadata for
			// the property, so it's mainly for user-defined beans.
			boolean readable = setter != null
					|| ClassUtils.getPackageName(parenType).equals(
							ClassUtils.getPackageName(type));
			return readable;
		}
	}

	/**
	 * Convenience class for grabbing and caching valid property names from
	 * /META-INF/spring-configuration-metadata.json so that metadata that is known to be
	 * valid can be used to pull the correct nested properties out of beans that might
	 * otherwise be tricky (contain cycles or other unserializable properties).
	 */
	protected static class ConfigurationPropertiesMetaData {

		private Map<String, Set<String>> matched = new HashMap<String, Set<String>>();
		private Set<String> keys = null;
		private String metadataLocations;

		public ConfigurationPropertiesMetaData(String metadataLocations) {
			this.metadataLocations = metadataLocations;
		}

		public boolean matches(String prefix) {
			if (this.matched.containsKey(prefix)) {
				return matchesInternal(prefix);
			}
			synchronized (this.matched) {
				if (this.matched.containsKey(prefix)) {
					return matchesInternal(prefix);
				}
				this.matched.put(prefix, findKeys(prefix));
			}
			return matchesInternal(prefix);
		}

		private boolean matchesInternal(String prefix) {
			if (this.matched.get(prefix) != null) {
				return true;
			}
			else {
				return false;
			}
		}

		private Set<String> findKeys(String prefix) {

			HashSet<String> set = new HashSet<String>();

			try {
				if (this.keys == null) {

					this.keys = new HashSet<String>();
					ObjectMapper mapper = new ObjectMapper();
					Resource[] resources = new PathMatchingResourcePatternResolver()
							.getResources(this.metadataLocations);
					for (Resource resource : resources) {
						addKeys(mapper, resource);
					}

				}
			}
			catch (IOException e) {
				logger.warn("Could not deserialize config properties metadata", e);
			}
			for (String key : this.keys) {
				if (key.length() > prefix.length()
						&& key.startsWith(prefix)
						&& ".".equals(key.substring(prefix.length(), prefix.length() + 1))) {
					set.add(key.substring(prefix.length() + 1));
				}
			}
			if (set.isEmpty()) {
				return null;
			}
			return set;
		}

		private void addKeys(ObjectMapper mapper, Resource resource) throws IOException,
				JsonParseException, JsonMappingException {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = mapper.readValue(resource.getInputStream(),
					Map.class);
			@SuppressWarnings("unchecked")
			Collection<Map<String, Object>> metadata = (Collection<Map<String, Object>>) map
					.get("properties");
			for (Map<String, Object> value : metadata) {
				try {
					if (value.containsKey("type")) {
						this.keys.add((String) value.get("name"));
					}
				}
				catch (Exception e) {
					logger.warn("Could not parse config properties metadata", e);
				}
			}
		}

		public Object extractMap(Object bean, String prefix) {
			if (!matches(prefix)) {
				return bean;
			}
			Map<String, Object> map = new HashMap<String, Object>();
			for (String key : this.matched.get(prefix)) {
				addProperty(bean, key, map);
			}
			return map;
		}

		private void addProperty(Object bean, String key, Map<String, Object> map) {
			String prefix = key.contains(".") ? StringUtils.split(key, ".")[0] : key;
			String suffix = key.length() > prefix.length() ? key.substring(prefix
					.length() + 1) : null;
			String property = prefix;
			if (bean instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> value = (Map<String, Object>) bean;
				bean = new MapHolder(value);
				property = "map[" + property + "]";
			}
			BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
			try {
				Object value = wrapper.getPropertyValue(property);
				if (value instanceof Map) {
					Map<String, Object> nested = new HashMap<String, Object>();
					map.put(prefix, nested);
					if (suffix != null) {
						addProperty(value, suffix, nested);
					}
				}
				else {
					map.put(prefix, value);
				}
			}
			catch (Exception e) {
				// Probably just lives on a different bean (it happens)
				logger.debug("Could not parse config properties metadata '" + key + "': "
						+ e.getMessage());
			}
		}

		protected static class MapHolder {
			Map<String, Object> map = new HashMap<String, Object>();

			public MapHolder(Map<String, Object> bean) {
				this.map.putAll(bean);
			}

			public Map<String, Object> getMap() {
				return this.map;
			}

			public void setMap(Map<String, Object> map) {
				this.map = map;
			}
		}

	}

}
