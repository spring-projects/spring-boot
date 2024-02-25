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

package org.springframework.boot.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for working with Env variables.
 *
 * @author Dmytro Nosan
 */
class EnvVariables {

	private final Map<String, String> variables;

	/**
     * Initializes the environment variables with the provided map of variables.
     * 
     * @param variables a map containing the environment variables as key-value pairs
     */
    EnvVariables(Map<String, String> variables) {
		this.variables = parseEnvVariables(variables);
	}

	/**
     * Parses the environment variables from the given map of arguments.
     * 
     * @param args the map of arguments containing environment variables
     * @return a map of parsed environment variables
     */
    private static Map<String, String> parseEnvVariables(Map<String, String> args) {
		if (args == null || args.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : args.entrySet()) {
			if (e.getKey() != null) {
				result.put(e.getKey(), getValue(e.getValue()));
			}
		}
		return result;
	}

	/**
     * Returns the value of the specified variable.
     * 
     * @param value the value of the variable
     * @return the value if it is not null, otherwise an empty string
     */
    private static String getValue(String value) {
		return (value != null) ? value : "";
	}

	/**
     * Returns an unmodifiable map of the variables in the environment.
     *
     * @return an unmodifiable map of the variables in the environment
     */
    Map<String, String> asMap() {
		return Collections.unmodifiableMap(this.variables);
	}

	/**
     * Converts the variables stored in the EnvVariables object into an array of strings.
     * Each variable is represented as a key-value pair in the format "key=value".
     * 
     * @return an array of strings representing the variables
     */
    String[] asArray() {
		List<String> args = new ArrayList<>(this.variables.size());
		for (Map.Entry<String, String> arg : this.variables.entrySet()) {
			args.add(arg.getKey() + "=" + arg.getValue());
		}
		return args.toArray(new String[0]);
	}

}
