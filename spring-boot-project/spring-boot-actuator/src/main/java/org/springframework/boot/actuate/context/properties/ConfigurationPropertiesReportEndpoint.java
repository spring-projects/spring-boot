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

package org.springframework.boot.actuate.context.properties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.context.properties.BoundConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * {@link Endpoint @Endpoint} to expose application properties from
 * {@link ConfigurationProperties @ConfigurationProperties} annotated beans.
 *
 * <p>
 * To protect sensitive information from being exposed, all property values are masked by
 * default. To configure when property values should be shown, use
 * {@code management.endpoint.configprops.show-values} and
 * {@code management.endpoint.configprops.roles} in your Spring Boot application
 * configuration.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author Chris Bono
 * @since 2.0.0
 */
@Endpoint(id = "configprops")
public class ConfigurationPropertiesReportEndpoint implements ApplicationContextAware {

	private static final String CONFIGURATION_PROPERTIES_FILTER_ID = "configurationPropertiesFilter";

	private final Sanitizer sanitizer;

	private final Show showValues;

	private ApplicationContext context;

	private ObjectMapper objectMapper;

	public ConfigurationPropertiesReportEndpoint(Iterable<SanitizingFunction> sanitizingFunctions, Show showValues) {
		this.sanitizer = new Sanitizer(sanitizingFunctions);
		this.showValues = showValues;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	@ReadOperation
	public ConfigurationPropertiesDescriptor configurationProperties() {
		boolean showUnsanitized = this.showValues.isShown(true);
		return getConfigurationProperties(showUnsanitized);
	}

	ConfigurationPropertiesDescriptor getConfigurationProperties(boolean showUnsanitized) {
		return getConfigurationProperties(this.context, (bean) -> true, showUnsanitized);
	}

	@ReadOperation
	public ConfigurationPropertiesDescriptor configurationPropertiesWithPrefix(@Selector String prefix) {
		boolean showUnsanitized = this.showValues.isShown(true);
		return getConfigurationProperties(prefix, showUnsanitized);
	}

	ConfigurationPropertiesDescriptor getConfigurationProperties(String prefix, boolean showUnsanitized) {
		return getConfigurationProperties(this.context, (bean) -> bean.getAnnotation().prefix().startsWith(prefix),
				showUnsanitized);
	}

	private ConfigurationPropertiesDescriptor getConfigurationProperties(ApplicationContext context,
			Predicate<ConfigurationPropertiesBean> beanFilterPredicate, boolean showUnsanitized) {
		ObjectMapper mapper = getObjectMapper();
		Map<String, ContextConfigurationPropertiesDescriptor> contexts = new HashMap<>();
		ApplicationContext target = context;

		while (target != null) {
			contexts.put(target.getId(), describeBeans(mapper, target, beanFilterPredicate, showUnsanitized));
			target = target.getParent();
		}
		return new ConfigurationPropertiesDescriptor(contexts);
	}

	private ObjectMapper getObjectMapper() {
		if (this.objectMapper == null) {
			JsonMapper.Builder builder = JsonMapper.builder();
			configureJsonMapper(builder);
			this.objectMapper = builder.build();
		}
		return this.objectMapper;
	}

	/**
	 * Configure Jackson's {@link JsonMapper} to be used to serialize the
	 * {@link ConfigurationProperties @ConfigurationProperties} objects into a {@link Map}
	 * structure.
	 * @param builder the json mapper builder
	 * @since 2.6.0
	 */
	protected void configureJsonMapper(JsonMapper.Builder builder) {
		builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		builder.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		builder.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
		builder.configure(MapperFeature.USE_STD_BEAN_NAMING, true);
		builder.serializationInclusion(Include.NON_NULL);
		applyConfigurationPropertiesFilter(builder);
		applySerializationModifier(builder);
		builder.addModule(new JavaTimeModule());
		builder.addModule(new ConfigurationPropertiesModule());
	}

	private void applyConfigurationPropertiesFilter(JsonMapper.Builder builder) {
		builder.annotationIntrospector(new ConfigurationPropertiesAnnotationIntrospector());
		builder
			.filterProvider(new SimpleFilterProvider().setDefaultFilter(new ConfigurationPropertiesPropertyFilter()));
	}

	/**
	 * Ensure only bindable and non-cyclic bean properties are reported.
	 * @param builder the JsonMapper builder
	 */
	private void applySerializationModifier(JsonMapper.Builder builder) {
		SerializerFactory factory = BeanSerializerFactory.instance
			.withSerializerModifier(new GenericSerializerModifier());
		builder.serializerFactory(factory);
	}

	private ContextConfigurationPropertiesDescriptor describeBeans(ObjectMapper mapper, ApplicationContext context,
			Predicate<ConfigurationPropertiesBean> beanFilterPredicate, boolean showUnsanitized) {
		Map<String, ConfigurationPropertiesBean> beans = ConfigurationPropertiesBean.getAll(context);
		Map<String, ConfigurationPropertiesBeanDescriptor> descriptors = beans.values()
			.stream()
			.filter(beanFilterPredicate)
			.collect(Collectors.toMap(ConfigurationPropertiesBean::getName,
					(bean) -> describeBean(mapper, bean, showUnsanitized)));
		return new ContextConfigurationPropertiesDescriptor(descriptors,
				(context.getParent() != null) ? context.getParent().getId() : null);
	}

	private ConfigurationPropertiesBeanDescriptor describeBean(ObjectMapper mapper, ConfigurationPropertiesBean bean,
			boolean showUnsanitized) {
		String prefix = bean.getAnnotation().prefix();
		Map<String, Object> serialized = safeSerialize(mapper, bean.getInstance(), prefix);
		Map<String, Object> properties = sanitize(prefix, serialized, showUnsanitized);
		Map<String, Object> inputs = getInputs(prefix, serialized, showUnsanitized);
		return new ConfigurationPropertiesBeanDescriptor(prefix, properties, inputs);
	}

	/**
	 * Cautiously serialize the bean to a map (returning a map with an error message
	 * instead of throwing an exception if there is a problem).
	 * @param mapper the object mapper
	 * @param bean the source bean
	 * @param prefix the prefix
	 * @return the serialized instance
	 */
	@SuppressWarnings({ "unchecked" })
	private Map<String, Object> safeSerialize(ObjectMapper mapper, Object bean, String prefix) {
		try {
			return new HashMap<>(mapper.convertValue(bean, Map.class));
		}
		catch (Exception ex) {
			return new HashMap<>(Collections.singletonMap("error", "Cannot serialize '" + prefix + "'"));
		}
	}

	/**
	 * Sanitize all unwanted configuration properties to avoid leaking of sensitive
	 * information.
	 * @param prefix the property prefix
	 * @param map the source map
	 * @param showUnsanitized whether to show the unsanitized values
	 * @return the sanitized map
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> sanitize(String prefix, Map<String, Object> map, boolean showUnsanitized) {
		map.forEach((key, value) -> {
			String qualifiedKey = getQualifiedKey(prefix, key);
			if (value instanceof Map) {
				map.put(key, sanitize(qualifiedKey, (Map<String, Object>) value, showUnsanitized));
			}
			else if (value instanceof List) {
				map.put(key, sanitize(qualifiedKey, (List<Object>) value, showUnsanitized));
			}
			else {
				map.put(key, sanitizeWithPropertySourceIfPresent(qualifiedKey, value, showUnsanitized));
			}
		});
		return map;
	}

	private Object sanitizeWithPropertySourceIfPresent(String qualifiedKey, Object value, boolean showUnsanitized) {
		ConfigurationPropertyName currentName = getCurrentName(qualifiedKey);
		ConfigurationProperty candidate = getCandidate(currentName);
		PropertySource<?> propertySource = getPropertySource(candidate);
		if (propertySource != null) {
			SanitizableData data = new SanitizableData(propertySource, qualifiedKey, value);
			return this.sanitizer.sanitize(data, showUnsanitized);
		}
		SanitizableData data = new SanitizableData(null, qualifiedKey, value);
		return this.sanitizer.sanitize(data, showUnsanitized);
	}

	private PropertySource<?> getPropertySource(ConfigurationProperty configurationProperty) {
		if (configurationProperty == null) {
			return null;
		}
		ConfigurationPropertySource source = configurationProperty.getSource();
		Object underlyingSource = (source != null) ? source.getUnderlyingSource() : null;
		return (underlyingSource instanceof PropertySource<?>) ? (PropertySource<?>) underlyingSource : null;
	}

	private ConfigurationPropertyName getCurrentName(String qualifiedKey) {
		return ConfigurationPropertyName.adapt(qualifiedKey, '.');
	}

	private ConfigurationProperty getCandidate(ConfigurationPropertyName currentName) {
		BoundConfigurationProperties bound = BoundConfigurationProperties.get(this.context);
		if (bound == null) {
			return null;
		}
		ConfigurationProperty candidate = bound.get(currentName);
		if (candidate == null && currentName.isLastElementIndexed()) {
			candidate = bound.get(currentName.chop(currentName.getNumberOfElements() - 1));
		}
		return candidate;
	}

	@SuppressWarnings("unchecked")
	private List<Object> sanitize(String prefix, List<Object> list, boolean showUnsanitized) {
		List<Object> sanitized = new ArrayList<>();
		int index = 0;
		for (Object item : list) {
			String name = prefix + "[" + index++ + "]";
			if (item instanceof Map) {
				sanitized.add(sanitize(name, (Map<String, Object>) item, showUnsanitized));
			}
			else if (item instanceof List) {
				sanitized.add(sanitize(name, (List<Object>) item, showUnsanitized));
			}
			else {
				sanitized.add(sanitizeWithPropertySourceIfPresent(name, item, showUnsanitized));
			}
		}
		return sanitized;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getInputs(String prefix, Map<String, Object> map, boolean showUnsanitized) {
		Map<String, Object> augmented = new LinkedHashMap<>(map);
		map.forEach((key, value) -> {
			String qualifiedKey = getQualifiedKey(prefix, key);
			if (value instanceof Map) {
				augmented.put(key, getInputs(qualifiedKey, (Map<String, Object>) value, showUnsanitized));
			}
			else if (value instanceof List) {
				augmented.put(key, getInputs(qualifiedKey, (List<Object>) value, showUnsanitized));
			}
			else {
				augmented.put(key, applyInput(qualifiedKey, showUnsanitized));
			}
		});
		return augmented;
	}

	@SuppressWarnings("unchecked")
	private List<Object> getInputs(String prefix, List<Object> list, boolean showUnsanitized) {
		List<Object> augmented = new ArrayList<>();
		int index = 0;
		for (Object item : list) {
			String name = prefix + "[" + index++ + "]";
			if (item instanceof Map) {
				augmented.add(getInputs(name, (Map<String, Object>) item, showUnsanitized));
			}
			else if (item instanceof List) {
				augmented.add(getInputs(name, (List<Object>) item, showUnsanitized));
			}
			else {
				augmented.add(applyInput(name, showUnsanitized));
			}
		}
		return augmented;
	}

	private Map<String, Object> applyInput(String qualifiedKey, boolean showUnsanitized) {
		ConfigurationPropertyName currentName = getCurrentName(qualifiedKey);
		ConfigurationProperty candidate = getCandidate(currentName);
		PropertySource<?> propertySource = getPropertySource(candidate);
		if (propertySource != null) {
			Object value = stringifyIfNecessary(candidate.getValue());
			SanitizableData data = new SanitizableData(propertySource, currentName.toString(), value);
			return getInput(candidate, this.sanitizer.sanitize(data, showUnsanitized));
		}
		return Collections.emptyMap();
	}

	private Map<String, Object> getInput(ConfigurationProperty candidate, Object sanitizedValue) {
		Map<String, Object> input = new LinkedHashMap<>();
		Origin origin = Origin.from(candidate);
		List<Origin> originParents = Origin.parentsFrom(candidate);
		input.put("value", sanitizedValue);
		input.put("origin", (origin != null) ? origin.toString() : "none");
		if (!originParents.isEmpty()) {
			input.put("originParents", originParents.stream().map(Object::toString).toArray(String[]::new));
		}
		return input;
	}

	private Object stringifyIfNecessary(Object value) {
		if (value == null || ClassUtils.isPrimitiveOrWrapper(value.getClass()) || value instanceof String) {
			return value;
		}
		if (CharSequence.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}
		return "Complex property value " + value.getClass().getName();
	}

	private String getQualifiedKey(String prefix, String key) {
		return (prefix.isEmpty() ? prefix : prefix + ".") + key;
	}

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	private static class ConfigurationPropertiesAnnotationIntrospector extends JacksonAnnotationIntrospector {

		@Override
		public Object findFilterId(Annotated a) {
			Object id = super.findFilterId(a);
			if (id == null) {
				id = CONFIGURATION_PROPERTIES_FILTER_ID;
			}
			return id;
		}

	}

	/**
	 * {@link SimpleBeanPropertyFilter} for serialization of
	 * {@link ConfigurationProperties @ConfigurationProperties} beans. The filter hides:
	 *
	 * <ul>
	 * <li>Properties that have a name starting with '$$'.
	 * <li>Properties that are self-referential.
	 * <li>Properties that throw an exception when retrieving their value.
	 * </ul>
	 */
	private static class ConfigurationPropertiesPropertyFilter extends SimpleBeanPropertyFilter {

		private static final Log logger = LogFactory.getLog(ConfigurationPropertiesPropertyFilter.class);

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

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
				PropertyWriter writer) throws Exception {
			if (writer instanceof BeanPropertyWriter beanPropertyWriter) {
				try {
					if (pojo == beanPropertyWriter.get(pojo)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Skipping '" + writer.getFullName() + "' on '" + pojo.getClass().getName()
									+ "' as it is self-referential");
						}
						return;
					}
				}
				catch (Exception ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping '" + writer.getFullName() + "' on '" + pojo.getClass().getName()
								+ "' as an exception was thrown when retrieving its value", ex);
					}
					return;
				}
			}
			super.serializeAsField(pojo, jgen, provider, writer);
		}

	}

	/**
	 * {@link SimpleModule} for configuring the serializer.
	 */
	private static final class ConfigurationPropertiesModule extends SimpleModule {

		private ConfigurationPropertiesModule() {
			addSerializer(DataSize.class, ToStringSerializer.instance);
		}

	}

	/**
	 * {@link BeanSerializerModifier} to return only relevant configuration properties.
	 */
	protected static class GenericSerializerModifier extends BeanSerializerModifier {

		private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
				List<BeanPropertyWriter> beanProperties) {
			List<BeanPropertyWriter> result = new ArrayList<>();
			Class<?> beanClass = beanDesc.getType().getRawClass();
			Bindable<?> bindable = Bindable.of(ClassUtils.getUserClass(beanClass));
			Constructor<?> bindConstructor = BindConstructorProvider.DEFAULT.getBindConstructor(bindable, false);
			for (BeanPropertyWriter writer : beanProperties) {
				if (isCandidate(beanDesc, writer, bindConstructor)) {
					result.add(writer);
				}
			}
			return result;
		}

		private boolean isCandidate(BeanDescription beanDesc, BeanPropertyWriter writer, Constructor<?> constructor) {
			if (constructor != null) {
				Parameter[] parameters = constructor.getParameters();
				String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(constructor);
				if (names == null) {
					names = new String[parameters.length];
				}
				for (int i = 0; i < parameters.length; i++) {
					String name = MergedAnnotations.from(parameters[i])
						.get(Name.class)
						.getValue(MergedAnnotation.VALUE, String.class)
						.orElse((names[i] != null) ? names[i] : parameters[i].getName());
					if (name.equals(writer.getName())) {
						return true;
					}
				}
			}
			return isReadable(beanDesc, writer);
		}

		private boolean isReadable(BeanDescription beanDesc, BeanPropertyWriter writer) {
			Class<?> parentType = beanDesc.getType().getRawClass();
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = findSetter(beanDesc, writer);
			// If there's a setter, we assume it's OK to report on the value,
			// similarly, if there's no setter but the package names match, we assume
			// that it is a nested class used solely for binding to config props, so it
			// should be kosher. Lists and Maps are also auto-detected by default since
			// that's what the metadata generator does. This filter is not used if there
			// is JSON metadata for the property, so it's mainly for user-defined beans.
			return (setter != null) || ClassUtils.getPackageName(parentType).equals(ClassUtils.getPackageName(type))
					|| Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
		}

		private AnnotatedMethod findSetter(BeanDescription beanDesc, BeanPropertyWriter writer) {
			String name = "set" + determineAccessorSuffix(writer.getName());
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = beanDesc.findMethod(name, new Class<?>[] { type });
			// The enabled property of endpoints returns a boolean primitive but is set
			// using a Boolean class
			if (setter == null && type.equals(Boolean.TYPE)) {
				setter = beanDesc.findMethod(name, new Class<?>[] { Boolean.class });
			}
			return setter;
		}

		/**
		 * Determine the accessor suffix of the specified {@code propertyName}, see
		 * section 8.8 "Capitalization of inferred names" of the JavaBean specs for more
		 * details.
		 * @param propertyName the property name to turn into an accessor suffix
		 * @return the accessor suffix for {@code propertyName}
		 */
		private String determineAccessorSuffix(String propertyName) {
			if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
				return propertyName;
			}
			return StringUtils.capitalize(propertyName);
		}

	}

	/**
	 * Description of an application's
	 * {@link ConfigurationProperties @ConfigurationProperties} beans.
	 */
	public static final class ConfigurationPropertiesDescriptor implements OperationResponseBody {

		private final Map<String, ContextConfigurationPropertiesDescriptor> contexts;

		ConfigurationPropertiesDescriptor(Map<String, ContextConfigurationPropertiesDescriptor> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextConfigurationPropertiesDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's
	 * {@link ConfigurationProperties @ConfigurationProperties} beans.
	 */
	public static final class ContextConfigurationPropertiesDescriptor {

		private final Map<String, ConfigurationPropertiesBeanDescriptor> beans;

		private final String parentId;

		private ContextConfigurationPropertiesDescriptor(Map<String, ConfigurationPropertiesBeanDescriptor> beans,
				String parentId) {
			this.beans = beans;
			this.parentId = parentId;
		}

		public Map<String, ConfigurationPropertiesBeanDescriptor> getBeans() {
			return this.beans;
		}

		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link ConfigurationProperties @ConfigurationProperties} bean.
	 */
	public static final class ConfigurationPropertiesBeanDescriptor {

		private final String prefix;

		private final Map<String, Object> properties;

		private final Map<String, Object> inputs;

		private ConfigurationPropertiesBeanDescriptor(String prefix, Map<String, Object> properties,
				Map<String, Object> inputs) {
			this.prefix = prefix;
			this.properties = properties;
			this.inputs = inputs;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public Map<String, Object> getProperties() {
			return this.properties;
		}

		public Map<String, Object> getInputs() {
			return this.inputs;
		}

	}

}
