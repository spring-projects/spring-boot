/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.bind.handler;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * {@link BindHandler} that falls back on a default value when a property could not be
 * bound (i.e. resolved to null).
 *
 * @author Tom Hombergs
 * @since 2.0.0
 */
public class FallbackBindHandler extends AbstractBindHandler {

	private final Map<ConfigurationPropertyName, ConfigurationPropertyName> fallbacks;

	public FallbackBindHandler(
			Map<ConfigurationPropertyName, ConfigurationPropertyName> fallbacks) {
		this.fallbacks = fallbacks;
	}

	@Override
	public Object onNull(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context) {
		ConfigurationPropertyName fallbackPropertyName = this.fallbacks.get(name);
		if (fallbackPropertyName != null) {
			ConfigurationProperty fallbackProperty = findProperty(fallbackPropertyName,
					context.streamSources());
			if (fallbackProperty != null && fallbackProperty.getValue() != null) {
				return fallbackProperty.getValue();
			}
		}
		return super.onNull(name, target, context);
	}

	private ConfigurationProperty findProperty(ConfigurationPropertyName name,
			Stream<ConfigurationPropertySource> sources) {
		return sources.map((source) -> source.getConfigurationProperty(name))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}
}
