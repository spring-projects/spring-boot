/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testutil;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Internal test utilities for setting environment values.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public abstract class EnvironmentTestUtils {

	public static void addEnvironment(ConfigurableApplicationContext context,
			String... pairs) {
		addEnvironment(context.getEnvironment(), pairs);
	}

	public static void addEnvironment(ConfigurableEnvironment environment,
			String... pairs) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map = getOrAdd(sources);
		for (String pair : pairs) {
			int index = getSeparatorIndex(pair);
			String key = pair.substring(0, index > 0 ? index : pair.length());
			String value = index > 0 ? pair.substring(index + 1) : "";
			map.put(key.trim(), value.trim());
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getOrAdd(MutablePropertySources sources) {
		if (sources.contains("test")) {
			return (Map<String, Object>) sources.get("test").getSource();
		}
		Map<String, Object> map = new HashMap<String, Object>();
		sources.addFirst(new MapPropertySource("test", map));
		return map;
	}

	private static int getSeparatorIndex(String pair) {
		int colonIndex = pair.indexOf(":");
		int equalIndex = pair.indexOf("=");
		if (colonIndex == -1) {
			return equalIndex;
		}
		if (equalIndex == -1) {
			return colonIndex;
		}
		return Math.min(colonIndex, equalIndex);
	}

}
