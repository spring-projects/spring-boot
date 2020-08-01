/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironmentContributor}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentContributorTests {

	private ConfigDataActivationContext activationContext = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
			null);

	@Test
	void getKindReturnsKind() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport("test");
		assertThat(contributor.getKind()).isEqualTo(Kind.INITIAL_IMPORT);
	}

	@Test
	void isActiveWhenPropertiesIsNullReturnsTrue() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport("test");
		assertThat(contributor.isActive(null)).isTrue();
	}

	@Test
	void isActiveWhenPropertiesIsActiveReturnsTrue() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.activate.on-cloud-platform", "kubernetes");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.isActive(this.activationContext)).isTrue();
	}

	@Test
	void isActiveWhenPropertiesIsNotActiveReturnsFalse() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.activate.on-cloud-platform", "heroku");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.isActive(this.activationContext)).isFalse();
	}

	@Test
	void getLocationReturnsLocation() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataLocation location = mock(ConfigDataLocation.class);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(location, configData,
				0, this.activationContext);
		assertThat(contributor.getLocation()).isSameAs(location);
	}

	@Test
	void getPropertySourceReturnsPropertySource() {
		MockPropertySource propertySource = new MockPropertySource();
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
		assertThat(contributor.getPropertySource()).isSameAs(propertySource);
	}

	@Test
	void getConfigurationPropertySourceReturnsAdaptedPropertySource() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring", "boot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.getConfigurationPropertySource()
				.getConfigurationProperty(ConfigurationPropertyName.of("spring")).getValue()).isEqualTo("boot");
	}

	@Test
	void getImportsWhenPropertiesIsNullReturnsEmptyList() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.getImports()).isEmpty();
	}

	@Test
	void getImportsReturnsImports() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "spring,boot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.getImports()).containsExactly("spring", "boot");
	}

	@Test
	void hasUnprocessedImportsWhenNoImportsReturnsFalse() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.hasUnprocessedImports(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isFalse();
	}

	@Test
	void hasUnprocessedImportsWhenHasNoChildrenForPhaseReturnsTrue() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "springboot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.hasUnprocessedImports(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isTrue();
	}

	@Test
	void hasUnprocessedImportsWhenHasChildrenForPhaseReturnsFalse() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "springboot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		ConfigData childConfigData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor childContributor = ConfigDataEnvironmentContributor.ofImported(null,
				childConfigData, 0, this.activationContext);
		ConfigDataEnvironmentContributor withChildren = contributor.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(childContributor));
		assertThat(withChildren.hasUnprocessedImports(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isFalse();
		assertThat(withChildren.hasUnprocessedImports(ImportPhase.AFTER_PROFILE_ACTIVATION)).isTrue();
	}

	@Test
	void getChildrenWhenHasNoChildrenReturnsEmptyList() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
		assertThat(contributor.getChildren(ImportPhase.AFTER_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void getChildrenWhenHasChildrenReturnsChildren() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "springboot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(null, configData, 0,
				this.activationContext);
		ConfigData childConfigData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor childContributor = ConfigDataEnvironmentContributor.ofImported(null,
				childConfigData, 0, this.activationContext);
		ConfigDataEnvironmentContributor withChildren = contributor.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(childContributor));
		assertThat(withChildren.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).containsExactly(childContributor);
		assertThat(withChildren.getChildren(ImportPhase.AFTER_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void streamReturnsStream() {
		ConfigDataEnvironmentContributor contributor = createContributor("a");
		Stream<String> stream = contributor.stream().map(this::getLocationName);
		assertThat(stream).containsExactly("a");
	}

	@Test
	void iteratorWhenSingleContributorReturnsSingletonIterator() {
		ConfigDataEnvironmentContributor contributor = createContributor("a");
		assertThat(asLocationsList(contributor.iterator())).containsExactly("a");
	}

	@Test
	void iteratorWhenTypicalStructureReturnsCorrectlyOrderedIterator() {
		ConfigDataEnvironmentContributor fileApplication = createContributor("file:application.properties");
		ConfigDataEnvironmentContributor fileProfile = createContributor("file:application-profile.properties");
		ConfigDataEnvironmentContributor fileImports = createContributor("file:./");
		fileImports = fileImports.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(fileApplication));
		fileImports = fileImports.withChildren(ImportPhase.AFTER_PROFILE_ACTIVATION,
				Collections.singletonList(fileProfile));
		ConfigDataEnvironmentContributor classpathApplication = createContributor("classpath:application.properties");
		ConfigDataEnvironmentContributor classpathProfile = createContributor(
				"classpath:application-profile.properties");
		ConfigDataEnvironmentContributor classpathImports = createContributor("classpath:/");
		classpathImports = classpathImports.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Arrays.asList(classpathApplication));
		classpathImports = classpathImports.withChildren(ImportPhase.AFTER_PROFILE_ACTIVATION,
				Arrays.asList(classpathProfile));
		ConfigDataEnvironmentContributor root = createContributor("root");
		root = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Arrays.asList(fileImports, classpathImports));
		assertThat(asLocationsList(root.iterator())).containsExactly("file:application-profile.properties",
				"file:application.properties", "file:./", "classpath:application-profile.properties",
				"classpath:application.properties", "classpath:/", "root");
	}

	@Test
	void withChildrenReturnsNewInstanceWithChildren() {
		ConfigDataEnvironmentContributor root = createContributor("root");
		ConfigDataEnvironmentContributor child = createContributor("child");
		ConfigDataEnvironmentContributor withChildren = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(child));
		assertThat(root.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
		assertThat(withChildren.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).containsExactly(child);
	}

	@Test
	void withReplacementReplacesChild() {
		ConfigDataEnvironmentContributor root = createContributor("root");
		ConfigDataEnvironmentContributor child = createContributor("child");
		ConfigDataEnvironmentContributor grandchild = createContributor("grandchild");
		child = child.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.singletonList(grandchild));
		root = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.singletonList(child));
		ConfigDataEnvironmentContributor updated = createContributor("updated");
		ConfigDataEnvironmentContributor withReplacement = root.withReplacement(grandchild, updated);
		assertThat(asLocationsList(root.iterator())).containsExactly("grandchild", "child", "root");
		assertThat(asLocationsList(withReplacement.iterator())).containsExactly("updated", "child", "root");
	}

	@Test
	void ofCreatesRootContributor() {
		ConfigDataEnvironmentContributor one = createContributor("one");
		ConfigDataEnvironmentContributor two = createContributor("two");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.of(Arrays.asList(one, two));
		assertThat(contributor.getKind()).isEqualTo(Kind.ROOT);
		assertThat(contributor.getLocation()).isNull();
		assertThat(contributor.getImports()).isEmpty();
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isNull();
		assertThat(contributor.getConfigurationPropertySource()).isNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).containsExactly(one, two);
	}

	@Test
	void ofInitialImportCreatedInitialImportContributor() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport("test");
		assertThat(contributor.getKind()).isEqualTo(Kind.INITIAL_IMPORT);
		assertThat(contributor.getLocation()).isNull();
		assertThat(contributor.getImports()).containsExactly("test");
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isNull();
		assertThat(contributor.getConfigurationPropertySource()).isNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void ofExistingCreatesExistingContributor() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "test");
		propertySource.setProperty("spring.config.activate.on-cloud-platform", "cloudfoundry");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
		assertThat(contributor.getKind()).isEqualTo(Kind.EXISTING);
		assertThat(contributor.getLocation()).isNull();
		assertThat(contributor.getImports()).isEmpty(); // Properties must not be bound
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void ofImportedCreatesImportedContributor() {
		TestLocation location = new TestLocation("test");
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "test");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(location, configData,
				0, this.activationContext);
		assertThat(contributor.getKind()).isEqualTo(Kind.IMPORTED);
		assertThat(contributor.getLocation()).isSameAs(location);
		assertThat(contributor.getImports()).containsExactly("test");
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void ofImportedWhenConfigDataHasIgnoreImportsOptionsCreatesImportedContributorWithoutImports() {
		TestLocation location = new TestLocation("test");
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "test");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource), ConfigData.Option.IGNORE_IMPORTS);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofImported(location, configData,
				0, this.activationContext);
		assertThat(contributor.getKind()).isEqualTo(Kind.IMPORTED);
		assertThat(contributor.getLocation()).isSameAs(location);
		assertThat(contributor.getImports()).isEmpty();
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void ofImportedWhenHasUseLegacyPropertyThrowsException() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.use-legacy-processing", "true");
		assertThatExceptionOfType(UseLegacyConfigProcessingException.class)
				.isThrownBy(() -> ConfigDataEnvironmentContributor.ofImported(null,
						new ConfigData(Collections.singleton(propertySource)), 0, this.activationContext));
	}

	private ConfigDataEnvironmentContributor createContributor(String location) {
		return ConfigDataEnvironmentContributor.ofImported(new TestLocation(location),
				new ConfigData(Collections.singleton(new MockPropertySource())), 0, this.activationContext);
	}

	private List<String> asLocationsList(Iterator<ConfigDataEnvironmentContributor> iterator) {
		List<String> list = new ArrayList<>();
		iterator.forEachRemaining((contributor) -> list.add(getLocationName(contributor)));
		return list;
	}

	private String getLocationName(ConfigDataEnvironmentContributor contributor) {
		return contributor.getLocation().toString();
	}

	static class TestLocation extends ConfigDataLocation {

		private final String location;

		TestLocation(String location) {
			this.location = location;
		}

		@Override
		public String toString() {
			return this.location;
		}

	}

}
