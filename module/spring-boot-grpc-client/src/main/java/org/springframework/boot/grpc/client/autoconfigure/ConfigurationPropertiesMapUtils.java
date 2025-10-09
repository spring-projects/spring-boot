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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Utility to help w/ conversion of configuration properties map values.
 * <p>
 * By default, Spring Boot turns a Yaml property whose value is a list of enum strings
 * (e.g. {@code statusCodes: [ "ONE, TWO" ]} into a map with integer keys. This utility
 * allows these integer keyed maps to instead be represented as list.
 *
 * @author Chris Bono
 */
final class ConfigurationPropertiesMapUtils {

	private static final String NUMBER = "[0-9]+";

	private ConfigurationPropertiesMapUtils() {
	}

	/**
	 * Converts any maps with integer keys into lists for the specified configuration map.
	 * @param input the configuration map to convert
	 * @return the map with integer keyed map values converted into lists
	 */
	static Map<String, @Nullable Object> convertIntegerKeyedMapsToLists(Map<String, @Nullable Object> input) {
		Map<String, @Nullable Object> map = new HashMap<>();
		for (Map.Entry<String, Object> entry : input.entrySet()) {
			map.put(entry.getKey(), extract(entry.getValue()));
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private static @Nullable Object extract(@Nullable Object input) {
		if (input == null) {
			return null;
		}
		if (input instanceof Map) {
			return map((Map<String, @Nullable Object>) input);
		}
		return input;
	}

	private static Object map(Map<String, @Nullable Object> input) {
		Map<String, Object> map = new HashMap<>();
		List<Object> list = new ArrayList<>();
		boolean maybeList = true;
		for (Map.Entry<String, Object> entry : input.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (maybeList && key.matches(NUMBER)) {
				int index = Integer.parseInt(key);
				while (index >= list.size()) {
					list.add(null);
				}
				list.set(index, extract(value));
			}
			else {
				maybeList = false;
				if (!list.isEmpty()) {
					// Not really a list after all
					for (int i = 0; i < list.size(); i++) {
						map.put(String.valueOf(i), list.get(i));
					}
					list.clear();
				}
				map.put(key, extract(value));
			}
		}
		if (list.size() > 0) {
			return list;
		}
		return map;
	}

}
