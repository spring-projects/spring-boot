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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigDataEnvironmentContributorPlaceholdersResolver}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentContributorPlaceholdersResolverTests {

	@Test
	void resolvePlaceholdersWhenNotStringReturnsResolved() {
		ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				Collections.emptyList(), null, null, false);
		assertThat(resolver.resolvePlaceholders(123)).isEqualTo(123);
	}

	@Test
	void resolvePlaceholdersWhenNotFoundReturnsOriginal() {
		ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				Collections.emptyList(), null, null, false);
		assertThat(resolver.resolvePlaceholders("${test}")).isEqualTo("${test}");
	}

	@Test
	void resolvePlaceholdersWhenFoundReturnsFirstMatch() {
		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>();
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s1", "nope", "t1"), true));
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s2", "test", "t2"), true));
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s3", "test", "t3"), true));
		ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				contributors, null, null, true);
		assertThat(resolver.resolvePlaceholders("${test}")).isEqualTo("t2");
	}

	@Test
	void resolvePlaceholdersWhenFoundInInactiveThrowsException() {
		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>();
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s1", "nope", "t1"), true));
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s2", "test", "t2"), true));
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s3", "test", "t3"), false));
		ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				contributors, null, null, true);
		assertThatExceptionOfType(InactiveConfigDataAccessException.class)
				.isThrownBy(() -> resolver.resolvePlaceholders("${test}"))
				.satisfies(propertyNameAndOriginOf("test", "s3"));
	}

	@Test
	void resolvePlaceholderWhenFoundInInactiveAndIgnoringReturnsResolved() {
		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>();
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s1", "nope", "t1"), true));
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s2", "test", "t2"), true));
		contributors.add(new TestConfigDataEnvironmentContributor(new TestPropertySource("s3", "test", "t3"), false));
		ConfigDataEnvironmentContributorPlaceholdersResolver resolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				contributors, null, null, false);
		assertThat(resolver.resolvePlaceholders("${test}")).isEqualTo("t2");
	}

	private Consumer<InactiveConfigDataAccessException> propertyNameAndOriginOf(String propertyName, String origin) {
		return (ex) -> {
			assertThat(ex.getPropertyName()).isEqualTo(propertyName);
			assertThat(((PropertySourceOrigin) (ex.getOrigin())).getPropertySource().getName()).isEqualTo(origin);
		};
	}

	static class TestPropertySource extends MapPropertySource implements OriginLookup<String> {

		TestPropertySource(String name, String key, String value) {
			this(name, Collections.singletonMap(key, value));
		}

		TestPropertySource(String name, Map<String, Object> source) {
			super(name, source);
		}

		@Override
		public Origin getOrigin(String key) {
			if (getSource().containsKey(key)) {
				return new PropertySourceOrigin(this, key);
			}
			return null;
		}

	}

	static class TestConfigDataEnvironmentContributor extends ConfigDataEnvironmentContributor {

		private final boolean active;

		protected TestConfigDataEnvironmentContributor(PropertySource<?> propertySource, boolean active) {
			super(Kind.ROOT, null, null, false, propertySource, null, null, null, null);
			this.active = active;
		}

		@Override
		boolean isActive(ConfigDataActivationContext activationContext) {
			return this.active;
		}

	}

}
