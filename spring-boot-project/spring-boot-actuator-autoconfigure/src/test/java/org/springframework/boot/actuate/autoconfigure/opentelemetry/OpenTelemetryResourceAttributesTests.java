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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

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
	void shouldUseUnknownServiceName() {
		Map<AttributeKey<?>, Object> attributes = apply();
		assertThat(attributes).hasSize(1);
		assertThat(attributes).containsEntry(AttributeKey.stringKey("service.name"), "unknown_service");
	}

	@Test
	void shouldUseServiceNameAngGroupFromSpringEnvironment() {
		this.environment.setProperty("spring.application.name", "test-service");
		this.environment.setProperty("spring.application.group", "test-group");

		Map<AttributeKey<?>, Object> attributes = apply();
		assertThat(attributes).hasSize(2);
		assertThat(attributes).containsEntry(AttributeKey.stringKey("service.name"), "test-service")
			.containsEntry(AttributeKey.stringKey("service.group"), "test-group");
	}

	@Test
	void shouldUseAttributesFromOtelEnvVariablesSpringApplicationNameAndGroupAndOtelServiceNameEnvAreIgnored() {
		this.environment.setProperty("spring.application.name", "ignored");
		this.environment.setProperty("spring.application.group", "ignored");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES",
				"key1=value1,key2=value2,key3,key4=,,service.name=otel-service,service.group=otel-group");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "ignored");

		Map<AttributeKey<?>, Object> attributes = apply();
		assertThat(attributes).hasSize(6);
		assertThat(attributes).containsEntry(AttributeKey.stringKey("key1"), "value1")
			.containsEntry(AttributeKey.stringKey("key2"), "value2")
			.containsEntry(AttributeKey.stringKey("key3"), "")
			.containsEntry(AttributeKey.stringKey("key4"), "")
			.containsEntry(AttributeKey.stringKey("service.name"), "otel-service")
			.containsEntry(AttributeKey.stringKey("service.group"), "otel-group");
	}

	@Test
	void shouldUseServiceNameFromOtelEnvVariableSpringApplicationNameIsIgnored() {
		this.environment.setProperty("spring.application.name", "ignored");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "otel-service");

		Map<AttributeKey<?>, Object> attributes = apply();
		assertThat(attributes).hasSize(3);
		assertThat(attributes).containsEntry(AttributeKey.stringKey("key1"), "value1")
			.containsEntry(AttributeKey.stringKey("key2"), "value2")
			.containsEntry(AttributeKey.stringKey("service.name"), "otel-service");
	}

	@Test
	void shouldResourceAttributesIgnoreEverythingElse() {
		this.resourceAttributes.put("service.name", "custom-service");
		this.resourceAttributes.put("service.group", "custom-group");
		this.environment.setProperty("spring.application.name", "ignored");
		this.environment.setProperty("spring.application.group", "ignored");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "ignored");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES",
				"key1=value1,key2=value2,service.name=ignored,service.group=ignored");

		Map<AttributeKey<?>, Object> attributes = apply();
		assertThat(attributes).hasSize(2);
		assertThat(attributes).containsEntry(AttributeKey.stringKey("service.name"), "custom-service")
			.containsEntry(AttributeKey.stringKey("service.group"), "custom-group");
	}

	private Map<AttributeKey<?>, Object> apply() {
		ResourceBuilder builder = Resource.builder();
		new OpenTelemetryResourceAttributes(this.environment, this.resourceAttributes, this.environmentVariables::get)
			.applyTo(builder);
		return builder.build().getAttributes().asMap();

	}

}
