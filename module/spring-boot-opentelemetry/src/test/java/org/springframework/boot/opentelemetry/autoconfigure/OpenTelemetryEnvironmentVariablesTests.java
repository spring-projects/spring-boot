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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.DeferredLogs;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryEnvironmentVariables.EnvVariable;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OpenTelemetryEnvironmentVariables}.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class OpenTelemetryEnvironmentVariablesTests {

	private DeferredLogs logFactory;

	@BeforeEach
	void setUp() {
		this.logFactory = new DeferredLogs();
	}

	@Test
	void getIntShouldReturnParsedValue() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "42"));
		EnvVariable result = env.getInt("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("VAR");
		assertThat(result.value()).isEqualTo("42");
	}

	@Test
	void getIntShouldReturnNoneWhenMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getInt("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getIntShouldReturnNoneWhenEmpty() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", ""));
		EnvVariable result = env.getInt("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getIntShouldReturnNoneAndWarnWhenNotANumber(CapturedOutput output) {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "abc"));
		EnvVariable result = env.getInt("VAR");
		assertThat(result.isPresent()).isFalse();
		assertThatLogContains(output, "Invalid value for integer environment variable 'VAR': 'abc'");
	}

	@Test
	void getIntWithFallbackShouldUsePrimaryWhenPresent() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("PRIMARY", "10", "FALLBACK", "20"));
		EnvVariable result = env.getInt("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("PRIMARY");
		assertThat(result.value()).isEqualTo("10");
	}

	@Test
	void getIntWithFallbackShouldUseFallbackWhenPrimaryMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("FALLBACK", "20"));
		EnvVariable result = env.getInt("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("FALLBACK");
		assertThat(result.value()).isEqualTo("20");
	}

	@Test
	void getIntWithFallbackShouldReturnNoneWhenBothMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getInt("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getBooleanShouldReturnTrueValue() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "true"));
		EnvVariable result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.value()).isEqualTo("true");
	}

	@Test
	void getBooleanShouldReturnFalseValue() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "false"));
		EnvVariable result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.value()).isEqualTo("false");
	}

	@Test
	void getBooleanShouldBeCaseInsensitive() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "TRUE"));
		EnvVariable result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.value()).isEqualTo("true");
		env = createEnv(Map.of("VAR", "False"));
		result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.value()).isEqualTo("false");
	}

	@Test
	void getBooleanShouldReturnNoneWhenMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getBooleanShouldReturnNoneWhenEmpty() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", ""));
		EnvVariable result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getBooleanShouldReturnNoneAndWarnWhenInvalid(CapturedOutput output) {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "yes"));
		EnvVariable result = env.getBoolean("VAR");
		assertThat(result.isPresent()).isFalse();
		assertThatLogContains(output, "Invalid value for boolean environment variable 'VAR': 'yes'");
	}

	@Test
	void getDurationShouldConvertMilliseconds() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "5000"));
		EnvVariable result = env.getDuration("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("VAR");
		assertThat(result.value()).isEqualTo("PT5S");
	}

	@Test
	void getDurationShouldHandleZero() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "0"));
		EnvVariable result = env.getDuration("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.value()).isEqualTo("PT0S");
	}

	@Test
	void getDurationShouldReturnNoneWhenMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getDuration("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getDurationShouldReturnNoneWhenEmpty() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", ""));
		EnvVariable result = env.getDuration("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getDurationShouldReturnNoneAndWarnWhenNotANumber(CapturedOutput output) {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "abc"));
		EnvVariable result = env.getDuration("VAR");
		assertThat(result.isPresent()).isFalse();
		assertThatLogContains(output, "Invalid value for duration environment variable 'VAR': 'abc'");
	}

	@Test
	void getDurationShouldReturnNoneAndWarnWhenNegative(CapturedOutput output) {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "-1"));
		EnvVariable result = env.getDuration("VAR");
		assertThat(result.isPresent()).isFalse();
		assertThatLogContains(output, "Duration environment variable 'VAR' must be non-negative, but was: '-1'");
	}

	@Test
	void getTimeoutShouldConvertMilliseconds() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "10000"));
		EnvVariable result = env.getTimeout("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("VAR");
		assertThat(result.value()).isEqualTo("PT10S");
	}

	@Test
	void getTimeoutShouldTreatZeroAsInfinity() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "0"));
		EnvVariable result = env.getTimeout("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.value()).isEqualTo(Duration.ofSeconds(Long.MAX_VALUE).toString());
	}

	@Test
	void getTimeoutShouldReturnNoneWhenMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getTimeout("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getTimeoutShouldReturnNoneWhenEmpty() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", ""));
		EnvVariable result = env.getTimeout("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getTimeoutShouldReturnNoneAndWarnWhenNotANumber(CapturedOutput output) {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "abc"));
		EnvVariable result = env.getTimeout("VAR");
		assertThat(result.isPresent()).isFalse();
		assertThatLogContains(output, "Invalid value for duration environment variable 'VAR': 'abc'");
	}

	@Test
	void getTimeoutShouldReturnNoneAndWarnWhenNegative(CapturedOutput output) {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "-5"));
		EnvVariable result = env.getTimeout("VAR");
		assertThat(result.isPresent()).isFalse();
		assertThatLogContains(output, "Duration environment variable 'VAR' must be non-negative, but was: '-5'");
	}

	@Test
	void getTimeoutWithFallbackShouldUsePrimaryWhenPresent() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("PRIMARY", "5000", "FALLBACK", "3000"));
		EnvVariable result = env.getTimeout("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("PRIMARY");
		assertThat(result.value()).isEqualTo("PT5S");
	}

	@Test
	void getTimeoutWithFallbackShouldUseFallbackWhenPrimaryMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("FALLBACK", "3000"));
		EnvVariable result = env.getTimeout("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("FALLBACK");
		assertThat(result.value()).isEqualTo("PT3S");
	}

	@Test
	void getTimeoutWithFallbackShouldReturnNoneWhenBothMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getTimeout("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getStringShouldReturnValue() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", "hello"));
		EnvVariable result = env.getString("VAR");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("VAR");
		assertThat(result.value()).isEqualTo("hello");
	}

	@Test
	void getStringShouldReturnNoneWhenMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getString("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getStringShouldReturnNoneWhenEmpty() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("VAR", ""));
		EnvVariable result = env.getString("VAR");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getStringWithFallbackShouldUsePrimaryWhenPresent() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("PRIMARY", "a", "FALLBACK", "b"));
		EnvVariable result = env.getString("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("PRIMARY");
		assertThat(result.value()).isEqualTo("a");
	}

	@Test
	void getStringWithFallbackShouldUseFallbackWhenPrimaryMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("FALLBACK", "b"));
		EnvVariable result = env.getString("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("FALLBACK");
		assertThat(result.value()).isEqualTo("b");
	}

	@Test
	void getStringWithFallbackShouldReturnNoneWhenBothMissing() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getString("PRIMARY", "FALLBACK");
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	void getStringWithFallbackTransformerShouldApplyTransformerToFallbackValue() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("FALLBACK", "base"));
		EnvVariable result = env.getString("PRIMARY", "FALLBACK", (value) -> value + "/appended");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("FALLBACK");
		assertThat(result.value()).isEqualTo("base/appended");
	}

	@Test
	void getStringWithFallbackTransformerShouldNotApplyTransformerToPrimaryValue() {
		OpenTelemetryEnvironmentVariables env = createEnv(Map.of("PRIMARY", "primary-value", "FALLBACK", "base"));
		EnvVariable result = env.getString("PRIMARY", "FALLBACK", (value) -> value + "/appended");
		assertThat(result.isPresent()).isTrue();
		assertThat(result.name()).isEqualTo("PRIMARY");
		assertThat(result.value()).isEqualTo("primary-value");
	}

	@Test
	void getFirstPresentShouldReturnFirstPresentVariable() {
		EnvVariable first = EnvVariable.of("A", "1");
		EnvVariable second = EnvVariable.of("B", "2");
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getFirstPresent(first, second);
		assertThat(result).isSameAs(first);
	}

	@Test
	void getFirstPresentShouldSkipAbsentVariables() {
		EnvVariable absent = EnvVariable.none();
		EnvVariable present = EnvVariable.of("B", "2");
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getFirstPresent(absent, present);
		assertThat(result).isSameAs(present);
	}

	@Test
	void getFirstPresentShouldReturnNullWhenAllAbsent() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		EnvVariable result = env.getFirstPresent(EnvVariable.none(), EnvVariable.none());
		assertThat(result).isNull();
	}

	@Test
	void getOriginShouldReturnOriginForPresentVariable() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		Origin origin = env.getOrigin(EnvVariable.of("MY_VAR", "value"));
		assertThat(origin).isNotNull();
		assertThat(origin).hasToString("Converted OpenTelemetry environment variable 'MY_VAR'");
	}

	@Test
	void getOriginShouldReturnNullForAbsentVariable() {
		OpenTelemetryEnvironmentVariables env = createEnv(Collections.emptyMap());
		Origin origin = env.getOrigin(EnvVariable.none());
		assertThat(origin).isNull();
	}

	@Test
	void envVariableNoneShouldNotBePresent() {
		EnvVariable none = EnvVariable.none();
		assertThat(none.isPresent()).isFalse();
		assertThat(none.name()).isNull();
		assertThat(none.value()).isNull();
	}

	@Test
	void addToMapShouldAddValueWithOrigin() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "value").addToMap(map, "my.property");
		assertThat(map).containsKey("my.property");
		assertThat(map.get("my.property")).satisfies((tracked) -> {
			assertThat(tracked.getValue()).isEqualTo("value");
			assertThat(tracked.getOrigin()).hasToString("Converted OpenTelemetry environment variable 'VAR'");
		});
	}

	@Test
	void addToMapWithTransformerShouldTransformValue() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "input").addToMap(map, "my.property", (name, value) -> value.toUpperCase(Locale.ROOT));
		assertThat(map).hasEntrySatisfying("my.property",
				(tracked) -> assertThat(tracked.getValue()).isEqualTo("INPUT"));
	}

	@Test
	void addToMapShouldNotAddWhenTransformerReturnsNull() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "input").addToMap(map, "my.property", (name, value) -> null);
		assertThat(map).isEmpty();
	}

	@Test
	void addToMapShouldBeNoOpForNoneVariable() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.none().addToMap(map, "my.property");
		assertThat(map).isEmpty();
	}

	@Test
	void addToMapShouldThrowOnDuplicateKey() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR1", "value1").addToMap(map, "my.property");
		assertThatIllegalStateException()
			.isThrownBy(() -> EnvVariable.of("VAR2", "value2").addToMap(map, "my.property"))
			.withMessageContaining("Duplicate key 'my.property'");
	}

	@Test
	void addMappingToMapShouldAddEntries() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "a=1,b=2").addMappingToMap(map, "my.headers", (name, value) -> {
			Map<String, String> result = new LinkedHashMap<>();
			for (String part : value.split(",")) {
				String[] keyValue = part.split("=", 2);
				result.put(keyValue[0], keyValue[1]);
			}
			return result;
		});
		assertThat(map).hasEntrySatisfying("my.headers[a]", (tracked) -> assertThat(tracked.getValue()).isEqualTo("1"));
		assertThat(map).hasEntrySatisfying("my.headers[b]", (tracked) -> assertThat(tracked.getValue()).isEqualTo("2"));
	}

	@Test
	void addMappingToMapShouldBeNoOpForNoneVariable() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.none().addMappingToMap(map, "my.headers", (name, value) -> Map.of("a", "1"));
		assertThat(map).isEmpty();
	}

	@Test
	void addMappingToMapShouldBeNoOpWhenTransformerReturnsNull() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "value").addMappingToMap(map, "my.headers", (name, value) -> null);
		assertThat(map).isEmpty();
	}

	@Test
	void addMappingToMapShouldThrowOnDuplicateKey() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR1", "v").addMappingToMap(map, "my.headers", (name, value) -> Map.of("a", "1"));
		assertThatIllegalStateException().isThrownBy(
				() -> EnvVariable.of("VAR2", "v").addMappingToMap(map, "my.headers", (name, value) -> Map.of("a", "2")))
			.withMessageContaining("Duplicate key 'my.headers[a]'");
	}

	@Test
	void addListToMapShouldAddEntries() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "a,b,c")
			.addListToMap(map, "my.list",
					(name, value) -> Stream.of(value.split(",")).map(String::toUpperCase).toList());
		assertThat(map).hasEntrySatisfying("my.list[0]", (tracked) -> assertThat(tracked.getValue()).isEqualTo("A"));
		assertThat(map).hasEntrySatisfying("my.list[1]", (tracked) -> assertThat(tracked.getValue()).isEqualTo("B"));
		assertThat(map).hasEntrySatisfying("my.list[2]", (tracked) -> assertThat(tracked.getValue()).isEqualTo("C"));
	}

	@Test
	void addListToMapShouldBeNoOpForNoneVariable() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.none().addListToMap(map, "my.list", (name, value) -> List.of("A"));
		assertThat(map).isEmpty();
	}

	@Test
	void addListToMapShouldBeNoOpWhenTransformerReturnsNull() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "value").addListToMap(map, "my.list", (name, value) -> null);
		assertThat(map).isEmpty();
	}

	@Test
	void addListToMapShouldAddEmptyStringWhenListIsEmpty() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR", "value").addListToMap(map, "my.list", (name, value) -> Collections.emptyList());
		assertThat(map).hasEntrySatisfying("my.list", (tracked) -> assertThat(tracked.getValue()).isEqualTo(""));
	}

	@Test
	void addListToMapShouldThrowOnDuplicateKey() {
		Map<String, OriginTrackedValue> map = new HashMap<>();
		EnvVariable.of("VAR1", "v").addListToMap(map, "my.list", (name, value) -> List.of("A"));
		assertThatIllegalStateException()
			.isThrownBy(() -> EnvVariable.of("VAR2", "v").addListToMap(map, "my.list", (name, value) -> List.of("B")))
			.withMessageContaining("Duplicate key 'my.list[0]'");
	}

	private OpenTelemetryEnvironmentVariables createEnv(Map<String, String> map) {
		return OpenTelemetryEnvironmentVariables.forMap(this.logFactory, map);
	}

	private void assertThatLogContains(CapturedOutput output, String message) {
		this.logFactory.switchOverAll();
		assertThat(output).contains(message);
	}

}
