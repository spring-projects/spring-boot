/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} for default locations of web resources.
 *
 * @author Stephane Nicoll
 * @since 3.0.0
 */
public class WebResourcesRuntimeHints implements RuntimeHintsRegistrar {

	private static final List<String> DEFAULT_LOCATIONS = List.of("META-INF/resources/", "resources/", "static/",
			"public/");

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		ClassLoader classLoaderToUse = (classLoader != null) ? classLoader : getClass().getClassLoader();
		String[] locations = DEFAULT_LOCATIONS.stream()
				.filter((candidate) -> classLoaderToUse.getResource(candidate) != null)
				.map((location) -> location + "*").toArray(String[]::new);
		if (locations.length > 0) {
			hints.resources().registerPattern((hint) -> hint.includes(locations));
		}
	}

}
