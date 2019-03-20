/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.cli.testutil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utilities for working with System properties in unit tests
 *
 * @author Andy Wilkinson
 */
public final class SystemProperties {

	private SystemProperties() {
	}

	/**
	 * Performs the given {@code action} with the given system properties set. System
	 * properties are restored to their previous values once the action has run.
	 * @param action The action to perform
	 * @param systemPropertyPairs The system properties, each in the form
	 * {@code key:value}
	 */
	public static void doWithSystemProperties(Runnable action,
			String... systemPropertyPairs) {
		Map<String, String> originalValues = new HashMap<String, String>();
		for (String pair : systemPropertyPairs) {
			String[] components = pair.split(":");
			String key = components[0];
			String value = components[1];
			originalValues.put(key, System.setProperty(key, value));
		}
		try {
			action.run();
		}
		finally {
			for (Entry<String, String> entry : originalValues.entrySet()) {
				if (entry.getValue() == null) {
					System.clearProperty(entry.getKey());
				}
				else {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
