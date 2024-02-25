/*
 * Copyright 2012-2024 the original author or authors.
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

	/**
     * Constructs a new ConfigurationPropertiesReportEndpoint with the specified sanitizing functions and show values.
     * 
     * @param sanitizingFunctions an Iterable of SanitizingFunction objects representing the sanitizing functions to be used
     * @param showValues a Show object representing the show values to be used
     */
    public ConfigurationPropertiesReportEndpoint(Iterable<SanitizingFunction> sanitizingFunctions, Show showValues) {
		this.sanitizer = new Sanitizer(sanitizingFunctions);
		this.showValues = showValues;
	}

	/**
     * Set the application context for this ConfigurationPropertiesReportEndpoint.
     * 
     * @param context the ApplicationContext to set
     * @throws BeansException if the context cannot be set
     */
    @Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	/**
     * Retrieves the configuration properties descriptor.
     * 
     * @return The configuration properties descriptor.
     */
    @ReadOperation
	public ConfigurationPropertiesDescriptor configurationProperties() {
		boolean showUnsanitized = this.showValues.isShown(true);
		return getConfigurationProperties(showUnsanitized);
	}

	/**
     * Retrieves the configuration properties descriptor.
     * 
     * @param showUnsanitized a boolean value indicating whether to show unsanitized properties
     * @return the configuration properties descriptor
     */
    ConfigurationPropertiesDescriptor getConfigurationProperties(boolean showUnsanitized) {
		return getConfigurationProperties(this.context, (bean) -> true, showUnsanitized);
	}

	/**
     * Retrieves the configuration properties with the specified prefix.
     * 
     * @param prefix The prefix used to filter the configuration properties.
     * @return The descriptor containing the configuration properties with the specified prefix.
     */
    @ReadOperation
	public ConfigurationPropertiesDescriptor configurationPropertiesWithPrefix(@Selector String prefix) {
		boolean showUnsanitized = this.showValues.isShown(true);
		return getConfigurationProperties(prefix, showUnsanitized);
	}

	/**
     * Retrieves the configuration properties descriptor for the given prefix.
     * 
     * @param prefix The prefix to filter the configuration properties by.
     * @param showUnsanitized Flag indicating whether to include unsanitized properties in the result.
     * @return The configuration properties descriptor matching the given prefix.
     */
    ConfigurationPropertiesDescriptor getConfigurationProperties(String prefix, boolean showUnsanitized) {
		return getConfigurationProperties(this.context, (bean) -> bean.getAnnotation().prefix().startsWith(prefix),
				showUnsanitized);
	}

	/**
     * Retrieves the configuration properties for the given application context.
     * 
     * @param context                The application context from which to retrieve the configuration properties.
     * @param beanFilterPredicate    A predicate used to filter the configuration properties beans.
     * @param showUnsanitized        A flag indicating whether to show unsanitized configuration properties.
     * @return                       The descriptor containing the configuration properties for the given context.
     */
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

	/**
     * Returns the ObjectMapper instance used for JSON serialization and deserialization.
     * If the ObjectMapper instance is not yet initialized, it will be created with the configured settings.
     *
     * @return the ObjectMapper instance
     */
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

	/**
     * Applies the configuration properties filter to the provided JsonMapper builder.
     * This filter is used to include only the properties annotated with @ConfigurationProperties
     * when serializing objects to JSON.
     *
     * @param builder the JsonMapper builder to apply the filter to
     */
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

	/**
     * Describes the beans in the application context that are annotated with @ConfigurationProperties.
     * 
     * @param mapper                The ObjectMapper used for serialization.
     * @param context               The ApplicationContext containing the beans.
     * @param beanFilterPredicate   The predicate used to filter the beans.
     * @param showUnsanitized       Flag indicating whether to show unsanitized values.
     * @return                      The descriptor containing the configuration properties of the beans.
     */
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

	/**
     * Describes a ConfigurationPropertiesBean by serializing its properties and inputs.
     * 
     * @param mapper           the ObjectMapper used for serialization
     * @param bean             the ConfigurationPropertiesBean to describe
     * @param showUnsanitized  a flag indicating whether to show unsanitized values
     * @return                 a ConfigurationPropertiesBeanDescriptor containing the serialized properties and inputs
     */
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

	/**
     * Sanitizes the given value with the property source if present.
     * 
     * @param qualifiedKey the qualified key of the property
     * @param value the value to be sanitized
     * @param showUnsanitized flag indicating whether to show unsanitized values
     * @return the sanitized value
     */
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

	/**
     * Retrieves the PropertySource associated with the given ConfigurationProperty.
     * 
     * @param configurationProperty the ConfigurationProperty to retrieve the PropertySource for
     * @return the PropertySource associated with the given ConfigurationProperty, or null if the ConfigurationProperty is null or if no PropertySource is found
     */
    private PropertySource<?> getPropertySource(ConfigurationProperty configurationProperty) {
		if (configurationProperty == null) {
			return null;
		}
		ConfigurationPropertySource source = configurationProperty.getSource();
		Object underlyingSource = (source != null) ? source.getUnderlyingSource() : null;
		return (underlyingSource instanceof PropertySource<?>) ? (PropertySource<?>) underlyingSource : null;
	}

	/**
     * Returns the current name of the configuration property based on the provided qualified key.
     * 
     * @param qualifiedKey the qualified key of the configuration property
     * @return the current name of the configuration property
     */
    private ConfigurationPropertyName getCurrentName(String qualifiedKey) {
		return ConfigurationPropertyName.adapt(qualifiedKey, '.');
	}

	/**
     * Retrieves the candidate ConfigurationProperty for the given ConfigurationPropertyName.
     * 
     * @param currentName the ConfigurationPropertyName for which the candidate ConfigurationProperty is to be retrieved
     * @return the candidate ConfigurationProperty, or null if not found
     */
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

	/**
     * Sanitizes the given list of objects by replacing sensitive information with placeholders.
     * 
     * @param prefix           the prefix to be used for generating the sanitized names
     * @param list             the list of objects to be sanitized
     * @param showUnsanitized  a flag indicating whether to show unsanitized values in the sanitized list
     * @return                 the sanitized list of objects
     */
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

	/**
     * Retrieves the inputs from the given map and returns a new map with the inputs augmented.
     * 
     * @param prefix           the prefix to be added to the qualified key
     * @param map              the map containing the inputs
     * @param showUnsanitized  a flag indicating whether to show unsanitized inputs
     * @return                 a new map with the inputs augmented
     */
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

	/**
     * Retrieves the inputs from the given list and returns a new list with the inputs augmented.
     * 
     * @param prefix           the prefix to be added to the input names
     * @param list             the list of objects containing the inputs
     * @param showUnsanitized  a boolean indicating whether to show unsanitized inputs
     * @return                 a new list with the inputs augmented
     */
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

	/**
     * Applies the input to the specified qualified key and returns a map of the sanitized input.
     * 
     * @param qualifiedKey the qualified key to apply the input to
     * @param showUnsanitized a boolean indicating whether to show unsanitized data
     * @return a map of the sanitized input
     */
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

	/**
     * Returns a map containing the input details of a configuration property.
     * 
     * @param candidate the configuration property to get the input details for
     * @param sanitizedValue the sanitized value of the configuration property
     * @return a map containing the input details of the configuration property
     */
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

	/**
     * Converts the given value to a string if necessary.
     * 
     * @param value the value to be converted
     * @return the converted value as a string, or the original value if it is null, a primitive or wrapper type, or a string
     * @throws NullPointerException if the value is null
     */
    private Object stringifyIfNecessary(Object value) {
		if (value == null || ClassUtils.isPrimitiveOrWrapper(value.getClass()) || value instanceof String) {
			return value;
		}
		if (CharSequence.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}
		return "Complex property value " + value.getClass().getName();
	}

	/**
     * Returns the qualified key by concatenating the prefix and key.
     * 
     * @param prefix the prefix to be added to the key (optional)
     * @param key the key to be qualified
     * @return the qualified key
     */
    private String getQualifiedKey(String prefix, String key) {
		return (prefix.isEmpty() ? prefix : prefix + ".") + key;
	}

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	private static final class ConfigurationPropertiesAnnotationIntrospector extends JacksonAnnotationIntrospector {

		/**
         * Overrides the {@code findFilterId} method in the parent class.
         * 
         * This method is used to find the filter ID for the given annotated element.
         * If the filter ID is not found, it defaults to {@code CONFIGURATION_PROPERTIES_FILTER_ID}.
         * 
         * @param a the annotated element
         * @return the filter ID for the annotated element
         */
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
	private static final class ConfigurationPropertiesPropertyFilter extends SimpleBeanPropertyFilter {

		private static final Log logger = LogFactory.getLog(ConfigurationPropertiesPropertyFilter.class);

		/**
         * Determines whether to include the specified BeanPropertyWriter in the filtering process.
         * 
         * @param writer the BeanPropertyWriter to be evaluated
         * @return true if the BeanPropertyWriter should be included, false otherwise
         */
        @Override
		protected boolean include(BeanPropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		/**
         * Determines whether to include the specified PropertyWriter in the filtering process.
         * 
         * @param writer the PropertyWriter to be included
         * @return true if the PropertyWriter should be included, false otherwise
         */
        @Override
		protected boolean include(PropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		/**
         * Checks if the given name should be included based on the filter criteria.
         * 
         * @param name the name to be checked
         * @return {@code true} if the name should be included, {@code false} otherwise
         */
        private boolean include(String name) {
			return !name.startsWith("$$");
		}

		/**
         * Serializes the given object as a field using the provided JsonGenerator and SerializerProvider.
         * This method is overridden to handle self-referential properties and skip them during serialization.
         * 
         * @param pojo     The object to be serialized as a field.
         * @param jgen     The JsonGenerator used for serialization.
         * @param provider The SerializerProvider used for serialization.
         * @param writer   The PropertyWriter representing the property to be serialized.
         * @throws Exception if an error occurs during serialization.
         */
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

		/**
         * Constructs a new ConfigurationPropertiesModule.
         * This module is responsible for configuring the properties of the application.
         * It adds a serializer for the DataSize class, using the ToStringSerializer instance.
         */
        private ConfigurationPropertiesModule() {
			addSerializer(DataSize.class, ToStringSerializer.instance);
		}

	}

	/**
	 * {@link BeanSerializerModifier} to return only relevant configuration properties.
	 */
	protected static class GenericSerializerModifier extends BeanSerializerModifier {

		private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

		/**
         * This method is used to change the properties of a bean during serialization.
         * It takes in the serialization configuration, bean description, and the list of bean property writers.
         * It returns a new list of bean property writers with the modified properties.
         *
         * @param config The serialization configuration.
         * @param beanDesc The bean description.
         * @param beanProperties The list of bean property writers.
         * @return The modified list of bean property writers.
         */
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

		/**
         * Checks if the given writer is a candidate for serialization based on the provided bean description and constructor.
         * 
         * @param beanDesc    The bean description of the object being serialized.
         * @param writer      The bean property writer being checked.
         * @param constructor The constructor of the object being serialized.
         * @return true if the writer is a candidate for serialization, false otherwise.
         */
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

		/**
         * Determines if a property is readable based on the provided bean description and writer.
         * 
         * @param beanDesc the bean description
         * @param writer the bean property writer
         * @return true if the property is readable, false otherwise
         */
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

		/**
         * Finds the setter method for a given BeanPropertyWriter in a BeanDescription.
         * 
         * @param beanDesc the BeanDescription object representing the bean class
         * @param writer the BeanPropertyWriter object representing the property
         * @return the AnnotatedMethod object representing the setter method, or null if not found
         */
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

		/**
         * Constructs a new ConfigurationPropertiesDescriptor with the specified contexts.
         *
         * @param contexts a map of context names to ContextConfigurationPropertiesDescriptor objects
         */
        ConfigurationPropertiesDescriptor(Map<String, ContextConfigurationPropertiesDescriptor> contexts) {
			this.contexts = contexts;
		}

		/**
         * Returns the map of context configuration properties descriptors.
         *
         * @return the map of context configuration properties descriptors
         */
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

		/**
         * Constructs a new instance of the {@code ContextConfigurationPropertiesDescriptor} class with the specified parameters.
         * 
         * @param beans
         *            a {@code Map} containing the bean descriptors for the configuration properties
         * @param parentId
         *            the ID of the parent configuration properties descriptor
         */
        private ContextConfigurationPropertiesDescriptor(Map<String, ConfigurationPropertiesBeanDescriptor> beans,
				String parentId) {
			this.beans = beans;
			this.parentId = parentId;
		}

		/**
         * Returns the map of beans in the context configuration properties.
         *
         * @return the map of beans, where the key is the bean name and the value is the bean descriptor
         */
        public Map<String, ConfigurationPropertiesBeanDescriptor> getBeans() {
			return this.beans;
		}

		/**
         * Returns the parent ID of the ContextConfigurationPropertiesDescriptor.
         *
         * @return the parent ID of the ContextConfigurationPropertiesDescriptor
         */
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

		/**
         * Constructs a new ConfigurationPropertiesBeanDescriptor with the specified prefix, properties, and inputs.
         * 
         * @param prefix the prefix to be used for the properties
         * @param properties the map of properties to be set
         * @param inputs the map of inputs to be set
         */
        private ConfigurationPropertiesBeanDescriptor(String prefix, Map<String, Object> properties,
				Map<String, Object> inputs) {
			this.prefix = prefix;
			this.properties = properties;
			this.inputs = inputs;
		}

		/**
         * Returns the prefix value of the ConfigurationPropertiesBeanDescriptor.
         *
         * @return the prefix value of the ConfigurationPropertiesBeanDescriptor
         */
        public String getPrefix() {
			return this.prefix;
		}

		/**
         * Returns the properties of the ConfigurationPropertiesBeanDescriptor.
         * 
         * @return a Map containing the properties of the ConfigurationPropertiesBeanDescriptor
         */
        public Map<String, Object> getProperties() {
			return this.properties;
		}

		/**
         * Returns the inputs of the ConfigurationPropertiesBeanDescriptor.
         *
         * @return a Map containing the inputs of the ConfigurationPropertiesBeanDescriptor
         */
        public Map<String, Object> getInputs() {
			return this.inputs;
		}

	}

}
