/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.util.CollectionUtils;

/**
 * Simple {@link HttpCodeStatusMapper} backed by map of {@link Status#getCode() status
 * code} to HTTP status code.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.2.0
 */
public class SimpleHttpCodeStatusMapper implements HttpCodeStatusMapper {

	private static final Map<String, Integer> DEFAULT_MAPPINGS;
	static {
		Map<String, Integer> defaultMappings = new HashMap<>();
		defaultMappings.put(Status.DOWN.getCode(), WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		defaultMappings.put(Status.OUT_OF_SERVICE.getCode(), WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		DEFAULT_MAPPINGS = getUniformMappings(defaultMappings);
	}

	private final Map<String, Integer> mappings;

	/**
	 * Create a new {@link SimpleHttpCodeStatusMapper} instance using default mappings.
	 */
	public SimpleHttpCodeStatusMapper() {
		this(null);
	}

	/**
	 * Create a new {@link SimpleHttpCodeStatusMapper} with the specified mappings.
	 * @param mappings the mappings to use or {@code null} to use the default mappings
	 */
	public SimpleHttpCodeStatusMapper(Map<String, Integer> mappings) {
		this.mappings = CollectionUtils.isEmpty(mappings) ? DEFAULT_MAPPINGS : getUniformMappings(mappings);
	}

	@Override
	public int getStatusCode(Status status) {
		String code = getUniformCode(status.getCode());
		return this.mappings.getOrDefault(code, WebEndpointResponse.STATUS_OK);
	}

	private static Map<String, Integer> getUniformMappings(Map<String, Integer> mappings) {
		Map<String, Integer> result = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
			String code = getUniformCode(entry.getKey());
			if (code != null) {
				result.putIfAbsent(code, entry.getValue());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static String getUniformCode(String code) {
		if (code == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (char ch : code.toCharArray()) {
			if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
				builder.append(Character.toLowerCase(ch));
			}
		}
		return builder.toString();
	}

}
