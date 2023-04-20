/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

/**
 * Parses and provides access to docker {@code env} data.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerEnv {

	private final Map<String, String> map;

	/**
	 * Create a new {@link DockerEnv} instance.
	 * @param env a list of env entries in the form {@code name=value} or {@code name}.
	 */
	DockerEnv(List<String> env) {
		this.map = parse(env);
	}

	private Map<String, String> parse(List<String> env) {
		if (CollectionUtils.isEmpty(env)) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new LinkedHashMap<>();
		env.stream().map(this::parseEntry).forEach((entry) -> result.put(entry.key(), entry.value()));
		return Collections.unmodifiableMap(result);
	}

	private Entry parseEntry(String entry) {
		int index = entry.indexOf('=');
		if (index != -1) {
			String key = entry.substring(0, index);
			String value = entry.substring(index + 1);
			return new Entry(key, value);
		}
		return new Entry(entry, null);
	}

	/**
	 * Return the env as a {@link Map}.
	 * @return the env as a map
	 */
	Map<String, String> asMap() {
		return this.map;
	}

	private record Entry(String key, String value) {

	}

}
