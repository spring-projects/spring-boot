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

package org.springframework.boot.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Test utilities for setting environment values.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @deprecated as of 1.4 in favor of
 * {@link org.springframework.boot.test.util.EnvironmentTestUtils}
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
		org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment(context,
				pairs);
	}

	/**
	 * Add additional (high priority) values to an {@link Environment}. Name-value pairs
	 * can be specified with colon (":") or equals ("=") separators.
	 * @param environment the environment to modify
	 * @param pairs the name:value pairs
	 */
	public static void addEnvironment(ConfigurableEnvironment environment,
			String... pairs) {
		org.springframework.boot.test.util.EnvironmentTestUtils
				.addEnvironment(environment, pairs);
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
		org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment(name,
				environment, pairs);
	}

}
