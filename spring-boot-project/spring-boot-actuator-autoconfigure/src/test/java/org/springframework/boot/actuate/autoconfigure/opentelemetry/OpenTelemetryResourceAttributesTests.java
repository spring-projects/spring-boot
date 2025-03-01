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

import org.springframework.mock.env.MockEnvironment;

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

	private final MockEnvironment environment = new MockEnvironment();

	private final Map<String, String> environmentVariables = new LinkedHashMap<>();

	private final Map<String, String> resourceAttributes = new LinkedHashMap<>();

	@BeforeAll
	static void beforeAll() {
		long seed = new Random().nextLong();
		random = new Random(seed);
	}

	@Test
	void otelServiceNameShouldTakePrecedenceOverOtelResourceAttributes() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "otel-service");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "otel-service");
	}

	@Test
	void otelServiceNameWhenEmptyShouldTakePrecedenceOverOtelResourceAttributes() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "");
	}

	@Test
	void otelResourceAttributes() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES",
				", ,,key1=value1,key2= value2, key3=value3,key4=,=value5,key6,=,key7=spring+boot,key8=ś");
		assertThat(getAttributes()).hasSize(7)
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2")
			.containsEntry("key3", "value3")
			.containsEntry("key4", "")
			.containsEntry("key7", "spring+boot")
			.containsEntry("key8", "ś")
			.containsEntry("service.name", "unknown_service");
	}

	@Test
	void resourceAttributesShouldBeMergedWithEnvironmentVariablesAndTakePrecedence() {
		this.resourceAttributes.put("service.group", "custom-group");
		this.resourceAttributes.put("key2", "");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "custom-service");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");
		assertThat(getAttributes()).hasSize(4)
			.containsEntry("service.name", "custom-service")
			.containsEntry("service.group", "custom-group")
			.containsEntry("key1", "value1")
			.containsEntry("key2", "");
	}

	@Test
	void invalidResourceAttributesShouldBeIgnored() {
		this.resourceAttributes.put("", "empty-key");
		this.resourceAttributes.put(null, "null-key");
		this.resourceAttributes.put("null-value", null);
		this.resourceAttributes.put("empty-value", "");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("empty-value", "");
	}

	@Test
	@SuppressWarnings("unchecked")
	void systemGetEnvShouldBeUsedAsDefaultEnvFunction() {
		OpenTelemetryResourceAttributes attributes = new OpenTelemetryResourceAttributes(this.environment, null);
		Function<String, String> getEnv = assertThat(attributes).extracting("getEnv")
			.asInstanceOf(InstanceOfAssertFactories.type(Function.class))
			.actual();
		System.getenv().forEach((key, value) -> assertThat(getEnv.apply(key)).isEqualTo(value));
	}

	@Test
	void otelResourceAttributeValuesShouldBePercentDecoded() {
		Stream.generate(this::generateRandomString).limit(10000).forEach((value) -> {
			this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=" + escaper.escape(value));
			assertThat(getAttributes()).hasSize(2)
				.containsEntry("service.name", "unknown_service")
				.containsEntry("key", value);
		});
	}

	@Test
	void illegalArgumentExceptionShouldBeThrownWhenDecodingIllegalHexCharPercentEncodedValue() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=abc%ß");
		assertThatIllegalArgumentException().isThrownBy(this::getAttributes)
			.withMessage("Failed to decode percent-encoded characters at index 3 in the value: 'abc%ß'");
	}

	@Test
	void replacementCharShouldBeUsedWhenDecodingNonUtf8Character() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=%a3%3e");
		assertThat(getAttributes()).containsEntry("key", "\ufffd>");
	}

	@Test
	void illegalArgumentExceptionShouldBeThrownWhenDecodingInvalidPercentEncodedValue() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=%");
		assertThatIllegalArgumentException().isThrownBy(this::getAttributes)
			.withMessage("Failed to decode percent-encoded characters at index 0 in the value: '%'");
	}

	@Test
	void unknownServiceShouldBeUsedAsDefaultServiceName() {
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "unknown_service");
	}

	@Test
	void springApplicationGroupNameShouldBeUsedAsDefaultServiceGroup() {
		this.environment.setProperty("spring.application.group", "spring-boot");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.group", "spring-boot");
	}

	@Test
	void springApplicationNameShouldBeUsedAsDefaultServiceName() {
		this.environment.setProperty("spring.application.name", "spring-boot-app");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "spring-boot-app");
	}

	@Test
	void resourceAttributesShouldTakePrecedenceOverSpringApplicationName() {
		this.resourceAttributes.put("service.name", "spring-boot");
		this.environment.setProperty("spring.application.name", "spring-boot-app");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "spring-boot");
	}

	@Test
	void otelResourceAttributesShouldTakePrecedenceOverSpringApplicationName() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=spring-boot");
		this.environment.setProperty("spring.application.name", "spring-boot-app");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "spring-boot");
	}

	@Test
	void otelServiceNameShouldTakePrecedenceOverSpringApplicationName() {
		this.environmentVariables.put("OTEL_SERVICE_NAME", "spring-boot");
		this.environment.setProperty("spring.application.name", "spring-boot-app");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "spring-boot");
	}

	@Test
	void resourceAttributesShouldTakePrecedenceOverSpringApplicationGroupName() {
		this.resourceAttributes.put("service.group", "spring-boot-app");
		this.environment.setProperty("spring.application.group", "spring-boot");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.group", "spring-boot-app");
	}

	@Test
	void otelResourceAttributesShouldTakePrecedenceOverSpringApplicationGroupName() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.group=spring-boot");
		this.environment.setProperty("spring.application.group", "spring-boot-app");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.group", "spring-boot");
	}

	private Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<>();
		new OpenTelemetryResourceAttributes(this.environment, this.resourceAttributes, this.environmentVariables::get)
			.applyTo(attributes::put);
		return attributes;
	}

	private String generateRandomString() {
		return random.ints(32, 127)
			.limit(64)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
	}

}
