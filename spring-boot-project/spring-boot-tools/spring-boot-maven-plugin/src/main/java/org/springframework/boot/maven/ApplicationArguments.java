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
import java.util.List;

/**
 * Utility class to parse and collect application arguments.
 *
 * @author Dmytro Nosan
 */
class ApplicationArguments {

	private final List<String> arguments;

	ApplicationArguments(String[] arguments) {
		this.arguments = parse(arguments);
	}

	String[] asArray() {
		return this.arguments.toArray(new String[0]);
	}

	private static List<String> parse(String[] args) {
		if (args == null || args.length == 0) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (String line : new RunArguments(String.join(",", args)).getArgs()) {
			String[] tokens = line.split(",--");
			result.add(tokens[0]);
			for (int i = 1; i < tokens.length; i++) {
				result.add("--" + tokens[i]);
			}
		}
		return Collections.unmodifiableList(result);
	}

}
