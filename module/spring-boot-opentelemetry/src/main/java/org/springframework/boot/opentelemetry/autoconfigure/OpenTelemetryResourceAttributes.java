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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link OpenTelemetryResourceAttributes} retrieves information from the
 * {@code OTEL_RESOURCE_ATTRIBUTES} and {@code OTEL_SERVICE_NAME} environment variables
 * and merges it with the resource attributes provided by the user. User-provided resource
 * attributes take precedence. Additionally, {@code spring.application.*} related
 * properties can be applied as defaults.
 * <p>
 * <a href= "https://opentelemetry.io/docs/specs/otel/resource/sdk/">OpenTelemetry
 * Resource Specification</a>
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
public class OpenTelemetryResourceAttributes {

	/**
	 * Default value for service name if {@code service.name} is not set.
	 */
	private static final String DEFAULT_SERVICE_NAME = "unknown_service";

	private final Environment environment;

	private final Map<String, String> resourceAttributes;

	private final Function<String, String> systemEnvironment;

	/**
	 * Creates a new instance of {@link OpenTelemetryResourceAttributes}.
	 * @param environment the environment
	 * @param resourceAttributes user-provided resource attributes to be used
	 */
	public OpenTelemetryResourceAttributes(Environment environment, Map<String, String> resourceAttributes) {
		this(environment, resourceAttributes, null);
	}

	/**
	 * Creates a new {@link OpenTelemetryResourceAttributes} instance.
	 * @param environment the environment
	 * @param resourceAttributes user-provided resource attributes to be used
	 * @param systemEnvironment a function to retrieve environment variables by name
	 */
	OpenTelemetryResourceAttributes(Environment environment, Map<String, String> resourceAttributes,
			Function<String, String> systemEnvironment) {
		Assert.notNull(environment, "'environment' must not be null");
		this.environment = environment;
		this.resourceAttributes = (resourceAttributes != null) ? resourceAttributes : Collections.emptyMap();
		this.systemEnvironment = (systemEnvironment != null) ? systemEnvironment : System::getenv;
	}

	/**
	 * Applies resource attributes to the provided {@link BiConsumer} after being combined
	 * from environment variables and user-defined resource attributes.
	 * <p>
	 * If a key exists in both environment variables and user-defined resources, the value
	 * from the user-defined resource takes precedence, even if it is empty.
	 * <p>
	 * Additionally, {@code spring.application.name} or {@code unknown_service} will be
	 * used as the default for {@code service.name}, and {@code spring.application.group}
	 * will serve as the default for {@code service.group} and {@code service.namespace}.
	 * @param consumer the {@link BiConsumer} to apply
	 */
	public void applyTo(BiConsumer<String, String> consumer) {
		Assert.notNull(consumer, "'consumer' must not be null");
		Map<String, String> attributes = getResourceAttributesFromEnv();
		this.resourceAttributes.forEach((name, value) -> {
			if (StringUtils.hasLength(name) && value != null) {
				attributes.put(name, value);
			}
		});
		attributes.computeIfAbsent("service.name", (key) -> getApplicationName());
		attributes.computeIfAbsent("service.namespace", (key) -> getServiceNamespace());
		attributes.forEach(consumer);
	}

	private String getApplicationName() {
		return this.environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME);
	}

	private String getServiceNamespace() {
		return this.environment.getProperty("spring.application.group");
	}

	/**
	 * Parses resource attributes from the {@link System#getenv()}. This method fetches
	 * attributes defined in the {@code OTEL_RESOURCE_ATTRIBUTES} and
	 * {@code OTEL_SERVICE_NAME} environment variables and provides them as key-value
	 * pairs.
	 * <p>
	 * If {@code service.name} is also provided in {@code OTEL_RESOURCE_ATTRIBUTES}, then
	 * {@code OTEL_SERVICE_NAME} takes precedence.
	 * @return resource attributes
	 */
	private Map<String, String> getResourceAttributesFromEnv() {
		Map<String, String> attributes = new LinkedHashMap<>();
		for (String attribute : StringUtils.tokenizeToStringArray(getEnv("OTEL_RESOURCE_ATTRIBUTES"), ",")) {
			int index = attribute.indexOf('=');
			if (index > 0) {
				String key = attribute.substring(0, index);
				String value = attribute.substring(index + 1);
				attributes.put(key.trim(), decode(value.trim()));
			}
		}
		String otelServiceName = getEnv("OTEL_SERVICE_NAME");
		if (otelServiceName != null) {
			attributes.put("service.name", otelServiceName);
		}
		return attributes;
	}

	private String getEnv(String name) {
		return this.systemEnvironment.apply(name);
	}

	/**
	 * Decodes a percent-encoded string. Converts sequences like '%HH' (where HH
	 * represents hexadecimal digits) back into their literal representations.
	 * <p>
	 * Inspired by {@code org.apache.commons.codec.net.PercentCodec}.
	 * @param value value to decode
	 * @return the decoded string
	 */
	private static String decode(String value) {
		if (value.indexOf('%') < 0) {
			return value;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if (b != '%') {
				out.write(b);
				continue;
			}
			int u = decodeHex(bytes, i + 1);
			int l = decodeHex(bytes, i + 2);
			if (u >= 0 && l >= 0) {
				out.write((u << 4) + l);
			}
			else {
				throw new IllegalArgumentException(
						"Failed to decode percent-encoded characters at index %d in the value: '%s'".formatted(i,
								value));
			}
			i += 2;
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	private static int decodeHex(byte[] bytes, int index) {
		return (index < bytes.length) ? Character.digit(bytes[index], 16) : -1;
	}

}
