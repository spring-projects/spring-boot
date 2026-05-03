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

package org.springframework.boot.opentelemetry.autoconfigure;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Reads and transforms OpenTelemetry environment variables.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryEnvironmentVariables {

	private final Log logger;

	private final Function<String, @Nullable String> envLookup;

	OpenTelemetryEnvironmentVariables(DeferredLogFactory deferredLogFactory,
			Function<String, @Nullable String> envLookup) {
		this.logger = deferredLogFactory.getLog(OpenTelemetryEnvironmentVariables.class);
		this.envLookup = envLookup;
	}

	EnvVariable getInt(String name) {
		// https://opentelemetry.io/docs/specs/otel/configuration/common/#integer
		String value = this.envLookup.apply(name);
		if (!StringUtils.hasLength(value)) {
			return EnvVariable.none();
		}
		try {
			return EnvVariable.of(name, Integer.toString(Integer.parseInt(value)));
		}
		catch (NumberFormatException ex) {
			this.logger.warn("Invalid value for integer environment variable '%s': '%s'".formatted(name, value));
			return EnvVariable.none();
		}
	}

	EnvVariable getInt(String name, String fallback) {
		EnvVariable envVariable = getInt(name);
		return (envVariable.isPresent()) ? envVariable : getInt(fallback);
	}

	EnvVariable getBoolean(String name) {
		// https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#boolean
		String value = this.envLookup.apply(name);
		if (!StringUtils.hasLength(value)) {
			return EnvVariable.none();
		}
		if (value.equalsIgnoreCase("true")) {
			return EnvVariable.of(name, "true");
		}
		if (value.equalsIgnoreCase("false")) {
			return EnvVariable.of(name, "false");
		}
		this.logger.warn("Invalid value for boolean environment variable '%s': '%s'".formatted(name, value));
		return EnvVariable.none();
	}

	EnvVariable getDuration(String name) {
		// https://opentelemetry.io/docs/specs/otel/configuration/common/#duration
		String value = this.envLookup.apply(name);
		if (!StringUtils.hasLength(value)) {
			return EnvVariable.none();
		}
		int intValue;
		try {
			intValue = Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			this.logger.warn("Invalid value for duration environment variable '%s': '%s'".formatted(name, value));
			return EnvVariable.none();
		}
		if (intValue < 0) {
			this.logger
				.warn("Duration environment variable '%s' must be non-negative, but was: '%s'".formatted(name, value));
			return EnvVariable.none();
		}
		return EnvVariable.of(name, Duration.ofMillis(intValue).toString());
	}

	EnvVariable getTimeout(String name) {
		// https://opentelemetry.io/docs/specs/otel/configuration/common/#timeout
		String value = this.envLookup.apply(name);
		if (!StringUtils.hasLength(value)) {
			return EnvVariable.none();
		}
		int intValue;
		try {
			intValue = Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			this.logger.warn("Invalid value for duration environment variable '%s': '%s'".formatted(name, value));
			return EnvVariable.none();
		}
		if (intValue < 0) {
			this.logger
				.warn("Duration environment variable '%s' must be non-negative, but was: '%s'".formatted(name, value));
			return EnvVariable.none();
		}
		if (intValue == 0) {
			return EnvVariable.of(name, Duration.ofSeconds(Long.MAX_VALUE).toString());
		}
		return EnvVariable.of(name, Duration.ofMillis(intValue).toString());
	}

	EnvVariable getTimeout(String name, String fallback) {
		EnvVariable envVariable = getTimeout(name);
		return (envVariable.isPresent()) ? envVariable : getTimeout(fallback);
	}

	EnvVariable getString(String name) {
		return getString(name, null, null);
	}

	EnvVariable getString(String name, String fallback) {
		return getString(name, fallback, null);
	}

	EnvVariable getString(String name, @Nullable String fallback,
			@Nullable Function<String, String> fallbackTransformer) {
		String value = this.envLookup.apply(name);
		if (StringUtils.hasLength(value)) {
			return EnvVariable.of(name, value);
		}
		if (fallback == null) {
			return EnvVariable.none();
		}
		value = this.envLookup.apply(fallback);
		if (!StringUtils.hasLength(value)) {
			return EnvVariable.none();
		}
		if (fallbackTransformer != null) {
			value = fallbackTransformer.apply(value);
		}
		return EnvVariable.of(fallback, value);
	}

	@Nullable Origin getOrigin(EnvVariable envVariable) {
		return OpenTelemetryEnvironmentVariableOrigin.from(envVariable);
	}

	@Nullable EnvVariable getFirstPresent(EnvVariable... variables) {
		for (EnvVariable variable : variables) {
			if (variable.isPresent()) {
				return variable;
			}
		}
		return null;
	}

	static OpenTelemetryEnvironmentVariables forSystemEnv(DeferredLogFactory deferredLogFactory) {
		return new OpenTelemetryEnvironmentVariables(deferredLogFactory, System::getenv);
	}

	static OpenTelemetryEnvironmentVariables forMap(DeferredLogFactory deferredLogFactory, Map<String, String> map) {
		return new OpenTelemetryEnvironmentVariables(deferredLogFactory, map::get);
	}

	record EnvVariable(@Nullable String name, @Nullable String value) {
		boolean isPresent() {
			return this.name != null && this.value != null;
		}

		static EnvVariable of(String name, String value) {
			return new EnvVariable(name, value);
		}

		static EnvVariable none() {
			return new EnvVariable(null, null);
		}

		void addToMap(Map<String, OriginTrackedValue> map, String key) {
			addToMap(map, key, (ignore, value) -> value);
		}

		void addToMap(Map<String, OriginTrackedValue> map, String key, ValueTransformer valueTransformer) {
			if (this.name == null || this.value == null) {
				return;
			}
			Assert.state(!map.containsKey(key), "Duplicate key '%s'".formatted(key));
			String value = valueTransformer.transform(this.name, this.value);
			if (value == null) {
				return;
			}
			map.put(key, OriginTrackedValue.of(value, OpenTelemetryEnvironmentVariableOrigin.from(this)));
		}

		void addMappingToMap(Map<String, OriginTrackedValue> map, String key, MapValueTransformer valueTransformer) {
			if (this.name == null || this.value == null) {
				return;
			}
			Map<String, String> value = valueTransformer.transform(this.name, this.value);
			if (value == null) {
				return;
			}
			Origin origin = OpenTelemetryEnvironmentVariableOrigin.from(this);
			for (Map.Entry<String, String> entry : value.entrySet()) {
				String entryKey = "%s[%s]".formatted(key, entry.getKey());
				Assert.state(!map.containsKey(entryKey), "Duplicate key '%s'".formatted(entryKey));
				map.put(entryKey, OriginTrackedValue.of(entry.getValue(), origin));
			}
		}

		void addListToMap(Map<String, OriginTrackedValue> map, String key, ListValueTransformer valueTransformer) {
			if (this.name == null || this.value == null) {
				return;
			}
			List<String> value = valueTransformer.transform(this.name, this.value);
			if (value == null) {
				return;
			}
			Origin origin = OpenTelemetryEnvironmentVariableOrigin.from(this);
			if (value.isEmpty()) {
				Assert.state(!map.containsKey(key), "Duplicate key '%s'".formatted(key));
				map.put(key, OriginTrackedValue.of("", origin));
				return;
			}
			for (int i = 0; i < value.size(); i++) {
				String entryKey = "%s[%d]".formatted(key, i);
				Assert.state(!map.containsKey(entryKey), "Duplicate key '%s'".formatted(entryKey));
				map.put(entryKey, OriginTrackedValue.of(value.get(i), origin));
			}
		}
	}

	@FunctionalInterface
	interface ValueTransformer {

		@Nullable String transform(String name, String value);

	}

	@FunctionalInterface
	interface MapValueTransformer {

		@Nullable Map<String, String> transform(String name, String value);

	}

	@FunctionalInterface
	interface ListValueTransformer {

		@Nullable List<String> transform(String name, String value);

	}

	private record OpenTelemetryEnvironmentVariableOrigin(String name) implements Origin {

		@Override
		public String toString() {
			return "Converted OpenTelemetry environment variable '%s'".formatted(this.name);
		}

		static @Nullable OpenTelemetryEnvironmentVariableOrigin from(EnvVariable envVariable) {
			if (!envVariable.isPresent()) {
				return null;
			}
			String name = envVariable.name();
			Assert.state(name != null, "'name' must not be null");
			return new OpenTelemetryEnvironmentVariableOrigin(name);
		}

	}

}
