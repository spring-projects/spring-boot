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

package org.springframework.boot.context.config;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.mock.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataLocationRuntimeHints}.
 *
 * @author Stephane Nicoll
 */
class ConfigDataLocationRuntimeHintsTests {

	@Test
	void registerWithDefaultSettings() {
		RuntimeHints hints = new RuntimeHints();
		new TestConfigDataLocationRuntimeHints().registerHints(hints, null);
		assertThat(hints.resources().resourcePatterns()).singleElement()
				.satisfies(includes("application*.properties", "application*.xml", "application*.yaml",
						"application*.yml", "config/application*.properties", "config/application*.xml",
						"config/application*.yaml", "config/application*.yml"));
	}

	@Test
	void registerWithCustomName() {
		RuntimeHints hints = new RuntimeHints();
		new TestConfigDataLocationRuntimeHints() {
			@Override
			protected List<String> getFileNames(ClassLoader classLoader) {
				return List.of("test");
			}

		}.registerHints(hints, null);
		assertThat(hints.resources().resourcePatterns()).singleElement()
				.satisfies(includes("test*.properties", "test*.xml", "test*.yaml", "test*.yml",
						"config/test*.properties", "config/test*.xml", "config/test*.yaml", "config/test*.yml"));
	}

	@Test
	void registerWithCustomLocation() {
		RuntimeHints hints = new RuntimeHints();
		new TestConfigDataLocationRuntimeHints() {
			@Override
			protected List<String> getLocations(ClassLoader classLoader) {
				return List.of("config/");
			}
		}.registerHints(hints, null);
		assertThat(hints.resources().resourcePatterns()).singleElement()
				.satisfies(includes("config/application*.properties", "config/application*.xml",
						"config/application*.yaml", "config/application*.yml"));
	}

	@Test
	void registerWithCustomExtension() {
		RuntimeHints hints = new RuntimeHints();
		new ConfigDataLocationRuntimeHints() {
			@Override
			protected List<String> getExtensions(ClassLoader classLoader) {
				return List.of(".conf");
			}
		}.registerHints(hints, null);
		assertThat(hints.resources().resourcePatterns()).singleElement()
				.satisfies(includes("application*.conf", "config/application*.conf"));
	}

	@Test
	void registerWithUnknownLocationDoesNotAddHint() {
		RuntimeHints hints = new RuntimeHints();
		new ConfigDataLocationRuntimeHints() {
			@Override
			protected List<String> getLocations(ClassLoader classLoader) {
				return List.of(UUID.randomUUID().toString());
			}
		}.registerHints(hints, null);
		assertThat(hints.resources().resourcePatterns()).isEmpty();
	}

	private Consumer<ResourcePatternHints> includes(String... patterns) {
		return (hint) -> {
			assertThat(hint.getIncludes().stream().map(ResourcePatternHint::getPattern))
					.containsExactlyInAnyOrder(patterns);
			assertThat(hint.getExcludes()).isEmpty();
		};
	}

	static class TestConfigDataLocationRuntimeHints extends ConfigDataLocationRuntimeHints {

		private final MockSpringFactoriesLoader springFactoriesLoader;

		TestConfigDataLocationRuntimeHints(MockSpringFactoriesLoader springFactoriesLoader) {
			this.springFactoriesLoader = springFactoriesLoader;
		}

		TestConfigDataLocationRuntimeHints() {
			this(springFactoriesLoader());
		}

		private static MockSpringFactoriesLoader springFactoriesLoader() {
			MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
			springFactoriesLoader.add(PropertySourceLoader.class, PropertiesPropertySourceLoader.class,
					YamlPropertySourceLoader.class);
			return springFactoriesLoader;
		}

		@Override
		protected SpringFactoriesLoader getSpringFactoriesLoader(ClassLoader classLoader) {
			return this.springFactoriesLoader;
		}

	}

}
