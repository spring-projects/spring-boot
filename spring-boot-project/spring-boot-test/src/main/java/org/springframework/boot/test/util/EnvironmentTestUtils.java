/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Test utilities for setting environment values.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 1.4.0
 * @deprecated since 2.0.0 in favor of {@link TestPropertyValues}
 */
@Deprecated
public abstract class EnvironmentTestUtils {

	/**
	 * Add additional (high priority) values to an {@link Environment} owned by an
	 * {@link ApplicationContext}. Name-value pairs can be specified with colon (":") or
	 * equals ("=") separators.
	 * @param context the context with an environment to modify
	 * @param pairs the name:value pairs
	 */
	public static void addEnvironment(ConfigurableApplicationContext context,
			String... pairs) {
		addEnvironment(context.getEnvironment(), pairs);
	}

	/**
	 * Add additional (high priority) values to an {@link Environment}. Name-value pairs
	 * can be specified with colon (":") or equals ("=") separators.
	 * @param environment the environment to modify
	 * @param pairs the name:value pairs
	 */
	public static void addEnvironment(ConfigurableEnvironment environment,
			String... pairs) {
		addEnvironment("test", environment, pairs);
	}

	/**
	 * Add additional (high priority) values to an {@link Environment}. Name-value pairs
	 * can be specified with colon (":") or equals ("=") separators.
	 * @param environment the environment to modify
	 * @param name the property source name
	 * @param pairs the name:value pairs
	 */
	public static void addEnvironment(String name, ConfigurableEnvironment environment,
			String... pairs) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map = getOrAdd(sources, name);
		for (String pair : pairs) {
			int index = getSeparatorIndex(pair);
			String key = (index > 0 ? pair.substring(0, index) : pair);
			String value = (index > 0 ? pair.substring(index + 1) : "");
			map.put(key.trim(), value.trim());
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getOrAdd(MutablePropertySources sources,
			String name) {
		if (sources.contains(name)) {
			return (Map<String, Object>) sources.get(name).getSource();
		}
		Map<String, Object> map = new HashMap<>();
		sources.addFirst(new MapPropertySource(name, map));
		return map;
	}

	private static int getSeparatorIndex(String pair) {
		int colonIndex = pair.indexOf(':');
		int equalIndex = pair.indexOf('=');
		if (colonIndex == -1) {
			return equalIndex;
		}
		if (equalIndex == -1) {
			return colonIndex;
		}
		return Math.min(colonIndex, equalIndex);
	}

}
