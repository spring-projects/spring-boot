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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.util.StringUtils;

/**
 * OpenTelemetryResourceAttributes retrieves information from the
 * {@code OTEL_RESOURCE_ATTRIBUTES} and {@code OTEL_SERVICE_NAME} environment variables
 * and merges it with the resource attributes provided by the user.
 * <p>
 * <b>User-provided resource attributes take precedence.</b>
 * <p>
 * <a href= "https://opentelemetry.io/docs/specs/otel/resource/sdk/">OpenTelemetry
 * Resource Specification</a>
 *
 * @author Dmytro Nosan
 * @since 3.5.0
 */
public final class OpenTelemetryResourceAttributes {

	private final Map<String, String> resourceAttributes;

	private final Function<String, String> getEnv;

	/**
	 * Creates a new instance of {@link OpenTelemetryResourceAttributes}.
	 * @param resourceAttributes user provided resource attributes to be used
	 */
	public OpenTelemetryResourceAttributes(Map<String, String> resourceAttributes) {
		this(resourceAttributes, null);
	}

	/**
	 * Creates a new {@link OpenTelemetryResourceAttributes} instance.
	 * @param resourceAttributes user provided resource attributes to be used
	 * @param getEnv a function to retrieve environment variables by name
	 */
	OpenTelemetryResourceAttributes(Map<String, String> resourceAttributes, Function<String, String> getEnv) {
		this.resourceAttributes = (resourceAttributes != null) ? resourceAttributes : Collections.emptyMap();
		this.getEnv = (getEnv != null) ? getEnv : System::getenv;
	}

	/**
	 * Returns resource attributes by combining attributes from environment variables and
	 * user-defined resource attributes. The final resource contains all attributes from
	 * both sources.
	 * <p>
	 * If a key exists in both environment variables and user-defined resources, the value
	 * from the user-defined resource takes precedence, even if it is empty.
	 * <p>
	 * <b>Null keys and values are ignored.</b>
	 * @return the resource attributes
	 */
	public Map<String, String> asMap() {
		Map<String, String> attributes = getResourceAttributesFromEnv();
		this.resourceAttributes.forEach((name, value) -> {
			if (name != null && value != null) {
				attributes.put(name, value);
			}
		});
		return attributes;
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
		return this.getEnv.apply(name);
	}

	/**
	 * Decodes a percent-encoded string. Converts sequences like '%HH' (where HH
	 * represents hexadecimal digits) back into their literal representations.
	 * <p>
	 * Inspired by {@code org.apache.commons.codec.net.PercentCodec}.
	 * @param value value to decode
	 * @return the decoded string
	 */
	public static String decode(String value) {
		if (value.indexOf('%') < 0) {
			return value;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if (b == '%') {
				int u = decodeHex(bytes, ++i);
				int l = decodeHex(bytes, ++i);
				if (u >= 0 && l >= 0) {
					bos.write((u << 4) + l);
				}
				else {
					throw new IllegalArgumentException(
							"Failed to decode percent-encoded characters at index %d in the value: '%s'"
								.formatted(i - 2, value));
				}
			}
			else {
				bos.write(b);
			}
		}
		return bos.toString(StandardCharsets.UTF_8);
	}

	private static int decodeHex(byte[] bytes, int index) {
		return (index < bytes.length) ? Character.digit(bytes[index], 16) : -1;
	}

}
