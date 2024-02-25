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

package org.springframework.boot.context.properties.source;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * {@link ConfigurationPropertyCaching} for an {@link Iterable iterable} set of
 * {@link ConfigurationPropertySource} instances.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesCaching implements ConfigurationPropertyCaching {

	private final Iterable<ConfigurationPropertySource> sources;

	/**
	 * Constructs a new ConfigurationPropertySourcesCaching object with the specified
	 * sources.
	 * @param sources the iterable collection of ConfigurationPropertySource objects to be
	 * cached
	 */
	ConfigurationPropertySourcesCaching(Iterable<ConfigurationPropertySource> sources) {
		this.sources = sources;
	}

	/**
	 * Enables the caching of configuration property sources. This method enables caching
	 * for each configuration property source by invoking the enable() method of the
	 * ConfigurationPropertyCaching class.
	 */
	@Override
	public void enable() {
		forEach(ConfigurationPropertyCaching::enable);
	}

	/**
	 * Disables the caching for all configuration property sources. This method iterates
	 * over each configuration property caching instance and calls the disable method.
	 */
	@Override
	public void disable() {
		forEach(ConfigurationPropertyCaching::disable);
	}

	/**
	 * Sets the time to live for all caching instances.
	 * @param timeToLive the time to live duration
	 */
	@Override
	public void setTimeToLive(Duration timeToLive) {
		forEach((caching) -> caching.setTimeToLive(timeToLive));
	}

	/**
	 * Clears all the configuration property sources caching. This method iterates over
	 * each configuration property caching and calls the clear method.
	 */
	@Override
	public void clear() {
		forEach(ConfigurationPropertyCaching::clear);
	}

	/**
	 * Iterates over each configuration property source and applies the specified action
	 * to the corresponding caching configuration property.
	 * @param action the action to be applied to each caching configuration property
	 */
	private void forEach(Consumer<ConfigurationPropertyCaching> action) {
		if (this.sources != null) {
			for (ConfigurationPropertySource source : this.sources) {
				ConfigurationPropertyCaching caching = CachingConfigurationPropertySource.find(source);
				if (caching != null) {
					action.accept(caching);
				}
			}
		}
	}

}
