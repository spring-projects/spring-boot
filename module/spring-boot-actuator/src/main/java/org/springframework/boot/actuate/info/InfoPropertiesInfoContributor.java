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

package org.springframework.boot.actuate.info;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.info.InfoProperties;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A base {@link InfoContributor} to expose an {@link InfoProperties}.
 *
 * @param <T> the type of the {@link InfoProperties} to expose
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.4.0
 */
public abstract class InfoPropertiesInfoContributor<T extends InfoProperties> implements InfoContributor {

	private static final Bindable<Map<String, Object>> STRING_OBJECT_MAP = Bindable.mapOf(String.class, Object.class);

	private final T properties;

	private final Mode mode;

	protected InfoPropertiesInfoContributor(T properties, Mode mode) {
		this.properties = properties;
		this.mode = mode;
	}

	/**
	 * Return the properties that this instance manages.
	 * @return the info properties
	 */
	protected final T getProperties() {
		return this.properties;
	}

	/**
	 * Return the mode that should be used to expose the content.
	 * @return the mode
	 */
	protected final Mode getMode() {
		return this.mode;
	}

	/**
	 * Return a {@link PropertySource} for the {@link Mode#SIMPLE SIMPLE} mode.
	 * @return the property source for the simple model
	 * @see #toPropertySource()
	 */
	protected abstract PropertySource<?> toSimplePropertySource();

	/**
	 * Extract the content to contribute to the info endpoint.
	 * @return the content to expose
	 * @see #extractContent(PropertySource)
	 * @see #postProcessContent(Map)
	 */
	protected Map<String, Object> generateContent() {
		Map<String, Object> content = extractContent(toPropertySource());
		postProcessContent(content);
		return content;
	}

	/**
	 * Extract the raw content based on the specified {@link PropertySource}.
	 * @param propertySource the property source to use
	 * @return the raw content
	 */
	protected Map<String, Object> extractContent(PropertySource<?> propertySource) {
		Iterable<@Nullable ConfigurationPropertySource> adapted = ConfigurationPropertySources.from(propertySource);
		return new Binder(ensureNonNullContent(adapted)).bind("", STRING_OBJECT_MAP).orElseGet(LinkedHashMap::new);
	}

	private Iterable<ConfigurationPropertySource> ensureNonNullContent(
			Iterable<@Nullable ConfigurationPropertySource> sources) {
		List<ConfigurationPropertySource> nonNullSources = new ArrayList<>(1);
		for (ConfigurationPropertySource source : sources) {
			Assert.notNull(source, "'source' must not be null");
			nonNullSources.add(source);
		}
		return nonNullSources;
	}

	/**
	 * Post-process the content to expose. Elements can be added, changed or removed.
	 * @param content the content to expose
	 */
	protected void postProcessContent(Map<String, Object> content) {

	}

	/**
	 * Return the {@link PropertySource} to use based on the chosen {@link Mode}.
	 * @return the property source
	 */
	protected PropertySource<?> toPropertySource() {
		if (this.mode.equals(Mode.FULL)) {
			return this.properties.toPropertySource();
		}
		return toSimplePropertySource();
	}

	/**
	 * Copy the specified key to the target {@link Properties} if it is set.
	 * @param target the target properties to update
	 * @param key the key
	 */
	protected void copyIfSet(Properties target, String key) {
		String value = this.properties.get(key);
		if (StringUtils.hasText(value)) {
			target.put(key, value);
		}
	}

	/**
	 * Replace the {@code value} for the specified key if the value is not {@code null}.
	 * @param content the content to expose
	 * @param key the property to replace
	 * @param value the new value
	 */
	protected void replaceValue(Map<String, Object> content, String key, @Nullable Object value) {
		if (content.containsKey(key) && value != null) {
			content.put(key, value);
		}
	}

	/**
	 * Return the nested map with the specified key or empty map if the specified map
	 * contains no mapping for the key.
	 * @param map the content
	 * @param key the key of a nested map
	 * @return the nested map
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			return Collections.emptyMap();
		}
		return (Map<String, Object>) value;
	}

	/**
	 * Defines how properties should be exposed.
	 */
	public enum Mode {

		/**
		 * Expose all available data, including custom properties.
		 */
		FULL,

		/**
		 * Expose a pre-defined set of core settings only.
		 */
		SIMPLE

	}

}
