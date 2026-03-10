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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * Parser for the W3C Baggage HTTP header as defined in the
 * <a href="https://www.w3.org/TR/baggage/#header-content">W3C Baggage specification</a>.
 *
 * @author Moritz Halbritter
 */
final class W3CHeaderParser {

	private W3CHeaderParser() {
	}

	/**
	 * Parses a W3C Baggage header value into a map of key-value pairs. Properties
	 * attached to list members are ignored. Values are percent-decoded.
	 * @param header the baggage header string to parse
	 * @return the baggage entries
	 */
	static Map<String, String> parse(String header) {
		if (!StringUtils.hasLength(header)) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new LinkedHashMap<>();
		for (String part : header.split(",", -1)) {
			part = part.strip();
			if (part.isEmpty()) {
				continue;
			}
			int semicolon = part.indexOf(';');
			String keyValue = (semicolon != -1) ? part.substring(0, semicolon) : part;
			int equals = keyValue.indexOf('=');
			if (equals == -1) {
				continue;
			}
			String key = keyValue.substring(0, equals).strip();
			String value = keyValue.substring(equals + 1).strip();
			if (key.isEmpty()) {
				continue;
			}
			result.put(key, percentDecode(value));
		}
		return result;
	}

	private static String percentDecode(String value) {
		if (value.indexOf('%') == -1) {
			return value;
		}
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

}
