/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.opentelemetry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import io.opentelemetry.api.internal.PercentEscaper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link OpenTelemetryResourceAttributes}.
 *
 * @author Dmytro Nosan
 */
class OpenTelemetryResourceAttributesTests {

	private static Random random;

	private static final PercentEscaper escaper = PercentEscaper.create();

	private final Map<String, String> environmentVariables = new LinkedHashMap<>();

	private final Map<String, String> resourceAttributes = new LinkedHashMap<>();

	@BeforeAll
	static void beforeAll() {
		long seed = new Random().nextLong();
		System.out.println("Seed: " + seed);
		random = new Random(seed);
	}

	@Test
	void otelServiceNameShouldTakePrecedenceOverOtelResourceAttributes() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "otel-service");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(1).containsEntry("service.name", "otel-service");
	}

	@Test
	void otelServiceNameWhenEmptyShouldTakePrecedenceOverOtelResourceAttributes() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(1).containsEntry("service.name", "");
	}

	@Test
	void otelResourceAttributesShouldBeUsed() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES",
				", ,,key1=value1,key2= value2, key3=value3,key4=,=value5,key6,=,key7=spring+boot,key8=ś");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(6)
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2")
			.containsEntry("key3", "value3")
			.containsEntry("key4", "")
			.containsEntry("key7", "spring+boot")
			.containsEntry("key8", "ś");
	}

	@Test
	void resourceAttributesShouldBeMergedWithEnvironmentVariables() {
		this.resourceAttributes.put("service.group", "custom-group");
		this.resourceAttributes.put("key2", "");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "custom-service");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(4)
			.containsEntry("service.name", "custom-service")
			.containsEntry("service.group", "custom-group")
			.containsEntry("key1", "value1")
			.containsEntry("key2", "");
	}

	@Test
	void resourceAttributesWithNullKeyOrValueShouldBeIgnored() {
		this.resourceAttributes.put("service.group", null);
		this.resourceAttributes.put("service.name", null);
		this.resourceAttributes.put(null, "value");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "custom-service");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(3)
			.containsEntry("service.name", "custom-service")
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2");
	}

	@Test
	@SuppressWarnings("unchecked")
	void systemGetEnvShouldBeUsedAsDefaultEnvFunctionAndResourceAttributesAreEmpty() {
		OpenTelemetryResourceAttributes attributes = new OpenTelemetryResourceAttributes(null);
		assertThat(attributes).extracting("resourceAttributes")
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.isNotNull()
			.isEmpty();
		Function<String, String> getEnv = assertThat(attributes).extracting("getEnv")
			.asInstanceOf(InstanceOfAssertFactories.type(Function.class))
			.actual();
		System.getenv().forEach((key, value) -> assertThat(getEnv.apply(key)).isEqualTo(value));
	}

	@Test
	void shouldDecodeOtelResourceAttributeValues() {
		Stream.generate(this::generateRandomString).limit(10000).forEach((value) -> {
			String key = "key";
			this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", key + "=" + escaper.escape(value));
			OpenTelemetryResourceAttributes attributes = getAttributes();
			assertThat(attributes.asMap()).hasSize(1).containsEntry(key, value);
		});
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenDecodingPercentIllegalHexChar() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=abc%ß");
		assertThatIllegalArgumentException().isThrownBy(() -> getAttributes().asMap())
			.withMessage("Failed to decode percent-encoded characters at index 3 in the value: 'abc%ß'");
	}

	@Test
	void shouldUseReplacementCharWhenDecodingNonUtf8Character() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=%a3%3e");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).containsEntry("key", "\ufffd>");
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenDecodingPercent() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=%");
		assertThatIllegalArgumentException().isThrownBy(() -> getAttributes().asMap())
			.withMessage("Failed to decode percent-encoded characters at index 0 in the value: '%'");
	}

	private OpenTelemetryResourceAttributes getAttributes() {
		return new OpenTelemetryResourceAttributes(this.resourceAttributes, this.environmentVariables::get);
	}

	private String generateRandomString() {
		return random.ints(32, 127)
			.limit(64)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
	}

}
