/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * Configuration data that has been loaded from an external {@link ConfigDataLocation
 * location} and may ultimately contribute {@link PropertySource property sources} to
 * Spring's {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 * @see ConfigDataLocationResolver
 * @see ConfigDataLoader
 */
public final class ConfigData {

	private final List<PropertySource<?>> propertySources;

	private final Set<Option> options;

	/**
	 * Create a new {@link ConfigData} instance.
	 * @param propertySources the config data property sources in ascending priority
	 * order.
	 * @param options the config data options
	 */
	public ConfigData(Collection<? extends PropertySource<?>> propertySources, Option... options) {
		Assert.notNull(propertySources, "PropertySources must not be null");
		Assert.notNull(options, "Options must not be null");
		this.propertySources = Collections.unmodifiableList(new ArrayList<>(propertySources));
		this.options = Collections.unmodifiableSet(
				(options.length != 0) ? EnumSet.copyOf(Arrays.asList(options)) : EnumSet.noneOf(Option.class));
	}

	/**
	 * Return the configuration data property sources in ascending priority order. If the
	 * same key is contained in more than one of the sources, then the later source will
	 * win.
	 * @return the config data property sources
	 */
	public List<PropertySource<?>> getPropertySources() {
		return this.propertySources;
	}

	/**
	 * Return a set of {@link Option config data options} for this source.
	 * @return the config data options
	 */
	public Set<Option> getOptions() {
		return this.options;
	}

	/**
	 * Option flags that can be applied config data.
	 */
	public enum Option {

		/**
		 * Ignore all imports properties from the sources.
		 */
		IGNORE_IMPORTS;

	}

}
