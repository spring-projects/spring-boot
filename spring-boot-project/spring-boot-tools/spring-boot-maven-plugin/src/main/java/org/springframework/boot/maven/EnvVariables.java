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

	EnvVariables(Map<String, String> variables) {
		this.variables = parseEnvVariables(variables);
	}

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

	private static String getValue(String value) {
		return (value != null) ? value : "";
	}

	public Map<String, String> asMap() {
		return Collections.unmodifiableMap(this.variables);
	}

	public String[] asArray() {
		List<String> args = new ArrayList<>(this.variables.size());
		for (Map.Entry<String, String> arg : this.variables.entrySet()) {
			args.add(arg.getKey() + "=" + arg.getValue());
		}
		return args.toArray(new String[0]);
	}

}
