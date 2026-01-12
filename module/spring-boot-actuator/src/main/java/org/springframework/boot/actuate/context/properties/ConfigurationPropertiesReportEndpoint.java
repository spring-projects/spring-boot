/*
 * Copyright 2012-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

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
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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

	private final Sanitizer sanitizer;

	private final Show showValues;

	private final BeanSerializer serializer;

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext context;

	public ConfigurationPropertiesReportEndpoint(Iterable<SanitizingFunction> sanitizingFunctions, Show showValues) {
		this.sanitizer = new Sanitizer(sanitizingFunctions);
		this.showValues = showValues;
		this.serializer = getBeanSerializer();
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
		Map<@Nullable String, ContextConfigurationPropertiesDescriptor> contexts = new HashMap<>();
		ApplicationContext target = context;

		while (target != null) {
			contexts.put(target.getId(), describeBeans(target, beanFilterPredicate, showUnsanitized));
			target = target.getParent();
		}
		return new ConfigurationPropertiesDescriptor(contexts);
	}

	@SuppressWarnings("removal")
	private static BeanSerializer getBeanSerializer() {
		ClassLoader classLoader = ConfigurationPropertiesReportEndpoint.class.getClassLoader();
		if (ClassUtils.isPresent("tools.jackson.databind.json.JsonMapper", classLoader)) {
			return new JacksonBeanSerializer();
		}
		if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)) {
			return new Jackson2BeanSerializer();
		}
		return (bean) -> {
			throw new IllegalStateException("Jackson is required for the 'configprops' endpoint");
		};
	}

	private ContextConfigurationPropertiesDescriptor describeBeans(ApplicationContext context,
			Predicate<ConfigurationPropertiesBean> beanFilterPredicate, boolean showUnsanitized) {
		Map<String, ConfigurationPropertiesBean> beans = ConfigurationPropertiesBean.getAll(context);
		Map<String, ConfigurationPropertiesBeanDescriptor> descriptors = beans.values()
			.stream()
			.filter(beanFilterPredicate)
			.collect(Collectors.toMap(ConfigurationPropertiesBean::getName,
					(bean) -> describeBean(bean, showUnsanitized)));
		return new ContextConfigurationPropertiesDescriptor(descriptors,
				(context.getParent() != null) ? context.getParent().getId() : null);
	}

	private ConfigurationPropertiesBeanDescriptor describeBean(ConfigurationPropertiesBean bean,
			boolean showUnsanitized) {
		String prefix = bean.getAnnotation().prefix();
		Map<String, @Nullable Object> serialized = safeSerialize(bean.getInstance(), prefix);
		Map<String, @Nullable Object> properties = sanitize(prefix, serialized, showUnsanitized);
		Map<String, Object> inputs = getInputs(prefix, serialized, showUnsanitized);
		return new ConfigurationPropertiesBeanDescriptor(prefix, properties, inputs);
	}

	/**
	 * Cautiously serialize the bean to a map (returning a map with an error message
	 * instead of throwing an exception if there is a problem).
	 * @param bean the source bean
	 * @param prefix the prefix
	 * @return the serialized instance
	 */
	private Map<String, @Nullable Object> safeSerialize(@Nullable Object bean, String prefix) {
		try {
			return new HashMap<>(this.serializer.serialize(bean));
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
	private Map<String, @Nullable Object> sanitize(String prefix, Map<String, @Nullable Object> map,
			boolean showUnsanitized) {
		map.forEach((key, value) -> {
			String qualifiedKey = getQualifiedKey(prefix, key);
			if (value instanceof Map) {
				map.put(key, sanitize(qualifiedKey, (Map<String, @Nullable Object>) value, showUnsanitized));
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

	private @Nullable Object sanitizeWithPropertySourceIfPresent(String qualifiedKey, @Nullable Object value,
			boolean showUnsanitized) {
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

	private @Nullable PropertySource<?> getPropertySource(@Nullable ConfigurationProperty configurationProperty) {
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

	private @Nullable ConfigurationProperty getCandidate(ConfigurationPropertyName currentName) {
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
	private List<@Nullable Object> sanitize(String prefix, List<Object> list, boolean showUnsanitized) {
		List<@Nullable Object> sanitized = new ArrayList<>();
		int index = 0;
		for (Object item : list) {
			String name = prefix + "[" + index++ + "]";
			if (item instanceof Map) {
				sanitized.add(sanitize(name, (Map<String, @Nullable Object>) item, showUnsanitized));
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
	private Map<String, Object> getInputs(String prefix, Map<String, @Nullable Object> map, boolean showUnsanitized) {
		Map<String, Object> augmented = new LinkedHashMap<>(map);
		map.forEach((key, value) -> {
			String qualifiedKey = getQualifiedKey(prefix, key);
			if (value instanceof Map) {
				augmented.put(key, getInputs(qualifiedKey, (Map<String, @Nullable Object>) value, showUnsanitized));
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
				augmented.add(getInputs(name, (Map<String, @Nullable Object>) item, showUnsanitized));
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

	private Map<String, @Nullable Object> applyInput(String qualifiedKey, boolean showUnsanitized) {
		ConfigurationPropertyName currentName = getCurrentName(qualifiedKey);
		ConfigurationProperty candidate = getCandidate(currentName);
		PropertySource<?> propertySource = getPropertySource(candidate);
		if (propertySource != null) {
			Assert.state(candidate != null, "'candidate' must not be null");
			Object value = stringifyIfNecessary(candidate.getValue());
			SanitizableData data = new SanitizableData(propertySource, currentName.toString(), value);
			return getInput(candidate, this.sanitizer.sanitize(data, showUnsanitized));
		}
		return Collections.emptyMap();
	}

	private Map<String, @Nullable Object> getInput(ConfigurationProperty candidate, @Nullable Object sanitizedValue) {
		Map<String, @Nullable Object> input = new LinkedHashMap<>();
		Origin origin = Origin.from(candidate);
		List<Origin> originParents = Origin.parentsFrom(candidate);
		input.put("value", sanitizedValue);
		input.put("origin", (origin != null) ? origin.toString() : "none");
		if (!originParents.isEmpty()) {
			input.put("originParents", originParents.stream().map(Object::toString).toArray(String[]::new));
		}
		return input;
	}

	private @Nullable Object stringifyIfNecessary(@Nullable Object value) {
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
	 * Description of an application's
	 * {@link ConfigurationProperties @ConfigurationProperties} beans.
	 */
	public static final class ConfigurationPropertiesDescriptor implements OperationResponseBody {

		private final Map<@Nullable String, ContextConfigurationPropertiesDescriptor> contexts;

		ConfigurationPropertiesDescriptor(Map<@Nullable String, ContextConfigurationPropertiesDescriptor> contexts) {
			this.contexts = contexts;
		}

		public Map<@Nullable String, ContextConfigurationPropertiesDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's
	 * {@link ConfigurationProperties @ConfigurationProperties} beans.
	 */
	public static final class ContextConfigurationPropertiesDescriptor {

		private final Map<String, ConfigurationPropertiesBeanDescriptor> beans;

		private final @Nullable String parentId;

		private ContextConfigurationPropertiesDescriptor(Map<String, ConfigurationPropertiesBeanDescriptor> beans,
				@Nullable String parentId) {
			this.beans = beans;
			this.parentId = parentId;
		}

		public Map<String, ConfigurationPropertiesBeanDescriptor> getBeans() {
			return this.beans;
		}

		public @Nullable String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link ConfigurationProperties @ConfigurationProperties} bean.
	 */
	public static final class ConfigurationPropertiesBeanDescriptor {

		private final String prefix;

		private final Map<String, @Nullable Object> properties;

		private final Map<String, Object> inputs;

		private ConfigurationPropertiesBeanDescriptor(String prefix, Map<String, @Nullable Object> properties,
				Map<String, Object> inputs) {
			this.prefix = prefix;
			this.properties = properties;
			this.inputs = inputs;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public Map<String, @Nullable Object> getProperties() {
			return this.properties;
		}

		public Map<String, Object> getInputs() {
			return this.inputs;
		}

	}

}
