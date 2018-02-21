/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;

/**
 * Base class for {@link DocumentMatcher DocumentMatchers} that check the
 * {@code spring.profiles} property.
 *
 * @author Phillip Webb
 * @see OriginTrackedYamlLoader
 */
abstract class SpringProfilesDocumentMatcher implements DocumentMatcher {

	@Override
	public final MatchStatus matches(Properties properties) {
		Binder binder = new Binder(
				new OriginTrackedValueConfigurationPropertySource(properties));
		String[] profiles = binder.bind("spring.profiles", Bindable.of(String[].class))
				.orElse(null);
		return (matches(profiles) ? MatchStatus.ABSTAIN : MatchStatus.NOT_FOUND);
	}

	protected abstract boolean matches(String[] profiles);

	/**
	 * {@link MapConfigurationPropertySource} that deals with unwrapping
	 * {@link OriginTrackedValue OriginTrackedValues} from the underlying map.
	 */
	static class OriginTrackedValueConfigurationPropertySource
			extends MapConfigurationPropertySource {

		OriginTrackedValueConfigurationPropertySource(Map<?, ?> map) {
			super(map);
		}

		@Override
		public ConfigurationProperty getConfigurationProperty(
				ConfigurationPropertyName name) {
			ConfigurationProperty property = super.getConfigurationProperty(name);
			if (property != null && property.getValue() instanceof OriginTrackedValue) {
				OriginTrackedValue originTrackedValue = (OriginTrackedValue) property
						.getValue();
				property = new ConfigurationProperty(property.getName(),
						originTrackedValue.getValue(), originTrackedValue.getOrigin());
			}
			return property;
		}

	}

}
