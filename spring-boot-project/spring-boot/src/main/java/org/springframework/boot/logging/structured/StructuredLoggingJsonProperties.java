/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Properties that can be used to customize structured logging JSON.
 *
 * @param include the paths that should be included. An empty set includes all names
 * @param exclude the paths that should be excluded. An empty set excludes nothing
 * @param rename a map of path to replacement names
 * @param add a map of additional elements {@link StructureLoggingJsonMembersCustomizer}
 * @param customizer the fully qualified name of a
 * {@link StructureLoggingJsonMembersCustomizer}
 * @author Phillip Webb
 */
record StructuredLoggingJsonProperties(Set<String> include, Set<String> exclude, Map<String, String> rename,
		Map<String, String> add, String customizer) {

	static StructuredLoggingJsonProperties get(Environment environment) {
		return Binder.get(environment)
			.bind("logging.structured.json", StructuredLoggingJsonProperties.class)
			.orElse(null);
	}

}
