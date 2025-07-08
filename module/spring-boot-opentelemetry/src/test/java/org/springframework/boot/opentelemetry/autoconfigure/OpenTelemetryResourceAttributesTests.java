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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.opentelemetry.api.internal.PercentEscaper;
import org.assertj.core.api.InstanceOfAssertFactories;
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

	private final MockEnvironment environment = new MockEnvironment();

	private final Map<String, String> environmentVariables = new LinkedHashMap<>();

	private final Map<String, String> resourceAttributes = new LinkedHashMap<>();

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
				", ,,key1=value1,key2= value2, key3=value3,key4=,=value5,key6,=,key7=%20spring+boot%20,key8=ś");
		assertThat(getAttributes()).hasSize(7)
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2")
			.containsEntry("key3", "value3")
			.containsEntry("key4", "")
			.containsEntry("key7", " spring+boot ")
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
		Function<String, String> getEnv = assertThat(attributes).extracting("systemEnvironment")
			.asInstanceOf(InstanceOfAssertFactories.type(Function.class))
			.actual();
		System.getenv().forEach((key, value) -> assertThat(getEnv.apply(key)).isEqualTo(value));
	}

	@Test
	void otelResourceAttributeValuesShouldBePercentDecoded() {
		PercentEscaper escaper = PercentEscaper.create();
		String value = IntStream.range(32, 127)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=" + escaper.escape(value));
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("key", value);
	}

	@Test
	void otelResourceAttributeValuesShouldBePercentDecodedWhenStringContainsNonAscii() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=%20\u015bp\u0159\u00ec\u0144\u0121%20");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("key", " śpřìńġ ");
	}

	@Test
	void otelResourceAttributeValuesShouldBePercentDecodedWhenMultiByteSequences() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key=T%C5%8Dky%C5%8D");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("key", "Tōkyō");
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
	void springApplicationGroupNameShouldBeUsedAsDefaultServiceNamespace() {
		this.environment.setProperty("spring.application.group", "spring-boot");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.namespace", "spring-boot");
	}

	@Test
	void springApplicationNameShouldBeUsedAsDefaultServiceName() {
		this.environment.setProperty("spring.application.name", "spring-boot-app");
		assertThat(getAttributes()).hasSize(1).containsEntry("service.name", "spring-boot-app");
	}

	@Test
	void serviceNamespaceShouldNotBePresentByDefault() {
		assertThat(getAttributes()).hasSize(1).doesNotContainKey("service.namespace");
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
		assertThat(getAttributes()).hasSize(3)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.group", "spring-boot-app");
	}

	@Test
	void resourceAttributesShouldTakePrecedenceOverApplicationGroupNameForPopulatingServiceNamespace() {
		this.resourceAttributes.put("service.namespace", "spring-boot-app");
		this.environment.setProperty("spring.application.group", "overridden");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.namespace", "spring-boot-app");
	}

	@Test
	void otelResourceAttributesShouldTakePrecedenceOverSpringApplicationGroupName() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.namespace=spring-boot");
		this.environment.setProperty("spring.application.group", "spring-boot-app");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.namespace", "spring-boot");
	}

	@Test
	void otelResourceAttributesShouldTakePrecedenceOverSpringApplicationGroupNameForServiceNamespace() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.namespace=spring-boot");
		this.environment.setProperty("spring.application.group", "overridden");
		assertThat(getAttributes()).hasSize(2)
			.containsEntry("service.name", "unknown_service")
			.containsEntry("service.namespace", "spring-boot");
	}

	@Test
	void shouldUseServiceGroupForServiceNamespaceIfServiceGroupIsSet() {
		this.environment.setProperty("spring.application.group", "alpha");
		assertThat(getAttributes()).containsEntry("service.namespace", "alpha");
	}

	@Test
	void shouldNotSetServiceNamespaceIfServiceGroupIsNotSet() {
		assertThat(getAttributes()).doesNotContainKey("service.namespace");
	}

	private Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<>();
		new OpenTelemetryResourceAttributes(this.environment, this.resourceAttributes, this.environmentVariables::get)
			.applyTo(attributes::put);
		return attributes;
	}

}
