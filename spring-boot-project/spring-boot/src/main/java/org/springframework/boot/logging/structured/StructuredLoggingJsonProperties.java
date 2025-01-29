/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.util.Instantiator;
import org.springframework.core.env.Environment;

/**
 * Properties that can be used to customize structured logging JSON.
 *
 * @param include the paths that should be included. An empty set includes all names
 * @param exclude the paths that should be excluded. An empty set excludes nothing
 * @param rename a map of path to replacement names
 * @param add a map of additional elements {@link StructuredLoggingJsonMembersCustomizer}
 * @param customizer the fully qualified names of
 * {@link StructuredLoggingJsonMembersCustomizer} implementations
 * @author Phillip Webb
 * @author Yanming Zhou
 */
record StructuredLoggingJsonProperties(Set<String> include, Set<String> exclude, Map<String, String> rename,
		Map<String, String> add, Set<Class<? extends StructuredLoggingJsonMembersCustomizer<?>>> customizer) {

	StructuredLoggingJsonProperties {
		customizer = (customizer != null) ? customizer : Collections.emptySet();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Collection<StructuredLoggingJsonMembersCustomizer<Object>> customizers(Instantiator<?> instantiator) {
		return (List) customizer().stream().map(instantiator::instantiateType).toList();
	}

	static StructuredLoggingJsonProperties get(Environment environment) {
		return Binder.get(environment)
			.bind("logging.structured.json", StructuredLoggingJsonProperties.class)
			.orElse(null);
	}

	static class StructuredLoggingJsonPropertiesRuntimeHints extends BindableRuntimeHintsRegistrar {

		StructuredLoggingJsonPropertiesRuntimeHints() {
			super(StructuredLoggingJsonProperties.class);
		}

	}

}
