/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnownAncestorsConfigurationPropertySource implements ConfigurationPropertySource {

	private final Map<ConfigurationPropertyName, ConfigurationPropertyState> ancestors = new HashMap<>();

	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		return null;
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		ConfigurationPropertyState state = this.ancestors.get(name);
		assertThat(state).isNotNull();
		return state;
	}

	KnownAncestorsConfigurationPropertySource absent(ConfigurationPropertyName name) {
		return ancestor(name, ConfigurationPropertyState.ABSENT);
	}

	KnownAncestorsConfigurationPropertySource present(ConfigurationPropertyName name) {
		return ancestor(name, ConfigurationPropertyState.PRESENT);
	}

	KnownAncestorsConfigurationPropertySource unknown(ConfigurationPropertyName name) {
		return ancestor(name, ConfigurationPropertyState.UNKNOWN);
	}

	private KnownAncestorsConfigurationPropertySource ancestor(ConfigurationPropertyName name,
			ConfigurationPropertyState state) {
		this.ancestors.put(name, state);
		return this;
	}

}
