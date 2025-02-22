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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A class responsible for managing and applying OpenTelemetry resource attributes. The
 * class supports resource definition from multiple sources:
 * <li>Resource attributes explicitly provided by
 * {@code management.opentelemetry.resource-attributes}</li>
 * <li>Environmental variables, such as {@code OTEL_RESOURCE_ATTRIBUTES} or
 * {@code OTEL_SERVICE_NAME}.</li>
 * <li>Spring application properties, such as {@code spring.application.name} or
 * {@code spring.application.group}.</li>
 * <p>
 * If {@code service.name} and {@code spring.application.name} are not explicitly set, the
 * service name defaults to "unknown_service".
 *
 * @author Dmytro Nosan
 */
final class OpenTelemetryResourceAttributes {

	/**
	 * Default value for application name if {@code spring.application.name} and
	 * {@code service.name} are not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "unknown_service";

	private final Environment environment;

	private final Map<String, String> resourceAttributes;

	private final Function<String, String> getEnv;

	OpenTelemetryResourceAttributes(Environment environment, Map<String, String> resourceAttributes) {
		this(environment, resourceAttributes, System::getenv);
	}

	OpenTelemetryResourceAttributes(Environment environment, Map<String, String> resourceAttributes,
			Function<String, String> getEnv) {
		this.environment = environment;
		this.resourceAttributes = (resourceAttributes != null) ? resourceAttributes : Collections.emptyMap();
		this.getEnv = getEnv;
	}

	/**
	 * Applies resource attributes to the provided {@code ResourceBuilder}.
	 * @param builder the {@code ResourceBuilder} to which the resource attributes will be
	 * applied
	 */
	void applyTo(ResourceBuilder builder) {
		getResourceAttributes().forEach(builder::put);
	}

	private Map<String, String> getResourceAttributes() {
		Map<String, String> attributes = new LinkedHashMap<>(this.resourceAttributes);
		if (CollectionUtils.isEmpty(attributes)) {
			attributes = getResourceAttributesFromEnv();
		}
		attributes.computeIfAbsent("service.name", (key) -> getApplicationName());
		attributes.computeIfAbsent("service.group", (key) -> getApplicationGroup());
		return attributes;
	}

	private String getApplicationGroup() {
		return this.environment.getProperty("spring.application.group");
	}

	private String getApplicationName() {
		return this.environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
	}

	/**
	 * Parses OpenTelemetry resource attributes from the {@link System#getenv()}. This
	 * method fetches attributes defined in the {@code OTEL_RESOURCE_ATTRIBUTES}
	 * environment variable and provides them as key-value pairs.
	 * <p>
	 * Additionally, if the {@code OTEL_SERVICE_NAME} environment variable is present and
	 * the {@code service.name} attribute is not explicitly set, it automatically includes
	 * {@code service.name} with the value from {@code OTEL_SERVICE_NAME}.
	 * @return the resource attributes
	 */
	private Map<String, String> getResourceAttributesFromEnv() {
		Map<String, String> attributes = new LinkedHashMap<>();
		for (String attribute : StringUtils.tokenizeToStringArray(getEnv("OTEL_RESOURCE_ATTRIBUTES"), ",")) {
			attribute = attribute.trim();
			if (!StringUtils.hasText(attribute)) {
				continue;
			}
			int index = attribute.indexOf('=');
			String key = (index > 0) ? attribute.substring(0, index) : attribute;
			String value = (index > 0) ? attribute.substring(index + 1) : "";
			attributes.put(key.trim(), value.trim());
		}
		String otelServiceName = getEnv("OTEL_SERVICE_NAME");
		if (otelServiceName != null && !attributes.containsKey("service.name")) {
			attributes.put("service.name", otelServiceName);
		}
		return attributes;
	}

	private String getEnv(String name) {
		return this.getEnv.apply(name);
	}

}
