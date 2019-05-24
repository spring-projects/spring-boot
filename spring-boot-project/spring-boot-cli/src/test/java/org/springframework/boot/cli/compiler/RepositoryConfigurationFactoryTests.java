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

package org.springframework.boot.cli.compiler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RepositoryConfigurationFactory}
 *
 * @author Andy Wilkinson
 */
class RepositoryConfigurationFactoryTests {

	@Test
	void defaultRepositories() {
		TestPropertyValues.of("user.home:src/test/resources/maven-settings/basic").applyToSystemProperties(() -> {
			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();
			assertRepositoryConfiguration(repositoryConfiguration, "central", "local", "spring-snapshot",
					"spring-milestone");
			return null;
		});
	}

	@Test
	void snapshotRepositoriesDisabled() {
		TestPropertyValues.of("user.home:src/test/resources/maven-settings/basic", "disableSpringSnapshotRepos:true")
				.applyToSystemProperties(() -> {
					List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
							.createDefaultRepositoryConfiguration();
					assertRepositoryConfiguration(repositoryConfiguration, "central", "local");
					return null;
				});
	}

	@Test
	void activeByDefaultProfileRepositories() {
		TestPropertyValues.of("user.home:src/test/resources/maven-settings/active-profile-repositories")
				.applyToSystemProperties(() -> {
					List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
							.createDefaultRepositoryConfiguration();
					assertRepositoryConfiguration(repositoryConfiguration, "central", "local", "spring-snapshot",
							"spring-milestone", "active-by-default");
					return null;
				});
	}

	@Test
	void activeByPropertyProfileRepositories() {
		TestPropertyValues.of("user.home:src/test/resources/maven-settings/active-profile-repositories", "foo:bar")
				.applyToSystemProperties(() -> {
					List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
							.createDefaultRepositoryConfiguration();
					assertRepositoryConfiguration(repositoryConfiguration, "central", "local", "spring-snapshot",
							"spring-milestone", "active-by-property");
					return null;
				});
	}

	@Test
	void interpolationProfileRepositories() {
		TestPropertyValues
				.of("user.home:src/test/resources/maven-settings/active-profile-repositories", "interpolate:true")
				.applyToSystemProperties(() -> {
					List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
							.createDefaultRepositoryConfiguration();
					assertRepositoryConfiguration(repositoryConfiguration, "central", "local", "spring-snapshot",
							"spring-milestone", "interpolate-releases", "interpolate-snapshots");
					return null;
				});
	}

	private void assertRepositoryConfiguration(List<RepositoryConfiguration> configurations, String... expectedNames) {
		assertThat(configurations).hasSize(expectedNames.length);
		Set<String> actualNames = new HashSet<>();
		for (RepositoryConfiguration configuration : configurations) {
			actualNames.add(configuration.getName());
		}
		assertThat(actualNames).containsOnly(expectedNames);
	}

}
