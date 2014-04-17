/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.test;

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
 */
public abstract class EnvironmentTestUtils {

	/**
	 * Add additional (high priority) values to an {@link Environment} owned by an
	 * {@link ApplicationContext}. Name-value pairs can be specified with colon (":") or
	 * equals ("=") separators.
	 * 
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
	 * 
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
	 * 
	 * @param environment the environment to modify
	 * @param name the property source name
	 * @param pairs the name:value pairs
	 */
	public static void addEnvironment(String name, ConfigurableEnvironment environment,
			String... pairs) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map;
		if (!sources.contains(name)) {
			map = new HashMap<String, Object>();
			MapPropertySource source = new MapPropertySource(name, map);
			sources.addFirst(source);
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Object> value = (Map<String, Object>) sources.get(name)
					.getSource();
			map = value;
		}
		for (String pair : pairs) {
			int index = pair.indexOf(":");
			index = index < 0 ? index = pair.indexOf("=") : index;
			String key = pair.substring(0, index > 0 ? index : pair.length());
			String value = index > 0 ? pair.substring(index + 1) : "";
			map.put(key.trim(), value.trim());
		}
	}

}
