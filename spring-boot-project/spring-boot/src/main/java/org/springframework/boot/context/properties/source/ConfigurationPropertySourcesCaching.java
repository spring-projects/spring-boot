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

	ConfigurationPropertySourcesCaching(Iterable<ConfigurationPropertySource> sources) {
		this.sources = sources;
	}

	@Override
	public void enable() {
		forEach(ConfigurationPropertyCaching::enable);
	}

	@Override
	public void disable() {
		forEach(ConfigurationPropertyCaching::disable);
	}

	@Override
	public void setTimeToLive(Duration timeToLive) {
		forEach((caching) -> caching.setTimeToLive(timeToLive));
	}

	@Override
	public void clear() {
		forEach(ConfigurationPropertyCaching::clear);
	}

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
