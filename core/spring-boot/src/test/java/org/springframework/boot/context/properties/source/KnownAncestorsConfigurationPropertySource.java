/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class KnownAncestorsConfigurationPropertySource implements ConfigurationPropertySource {

	private final Map<ConfigurationPropertyName, ConfigurationPropertyState> ancestors = new HashMap<>();

	@Override
	public @Nullable ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
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
