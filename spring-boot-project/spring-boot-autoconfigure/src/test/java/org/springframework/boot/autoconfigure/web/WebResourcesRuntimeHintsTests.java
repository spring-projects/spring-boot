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

package org.springframework.boot.autoconfigure.web;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebResourcesRuntimeHints}.
 *
 * @author Stephane Nicoll
 */
class WebResourcesRuntimeHintsTests {

	@Test
	void registerHintsWithAllLocations() {
		RuntimeHints hints = register(
				new TestClassLoader(List.of("META-INF/resources/", "resources/", "static/", "public/")));
		assertThat(hints.resources().resourcePatternHints()).singleElement()
			.satisfies(include("META-INF/resources/*", "resources/*", "static/*", "public/*"));
	}

	@Test
	void registerHintsWithOnlyStaticLocations() {
		RuntimeHints hints = register(new TestClassLoader(List.of("static/")));
		assertThat(hints.resources().resourcePatternHints()).singleElement().satisfies(include("static/*"));
	}

	@Test
	void registerHintsWithNoLocation() {
		RuntimeHints hints = register(new TestClassLoader(Collections.emptyList()));
		assertThat(hints.resources().resourcePatternHints()).isEmpty();
	}

	private RuntimeHints register(ClassLoader classLoader) {
		RuntimeHints hints = new RuntimeHints();
		WebResourcesRuntimeHints registrar = new WebResourcesRuntimeHints();
		registrar.registerHints(hints, classLoader);
		return hints;
	}

	private Consumer<ResourcePatternHints> include(String... patterns) {
		return (hint) -> {
			assertThat(hint.getIncludes()).map(ResourcePatternHint::getPattern).contains(patterns);
			assertThat(hint.getExcludes()).isEmpty();
		};
	}

	private static class TestClassLoader extends URLClassLoader {

		private final List<String> availableResources;

		TestClassLoader(List<String> availableResources) {
			super(new URL[0], TestClassLoader.class.getClassLoader());
			this.availableResources = availableResources;
		}

		@Override
		public URL getResource(String name) {
			return (this.availableResources.contains(name)) ? super.getResource("web/custom-resource.txt") : null;
		}

	}

}
