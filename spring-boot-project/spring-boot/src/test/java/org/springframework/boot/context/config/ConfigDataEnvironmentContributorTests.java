/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.boot.context.config.ConfigData.Option;
import org.springframework.boot.context.config.ConfigData.PropertySourceOptions;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.properties.bind.Binder;
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

	private static final ConfigDataLocation TEST_LOCATION = ConfigDataLocation.of("test");

	private ConfigDataActivationContext activationContext = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
			null);

	@Test
	void getKindReturnsKind() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(TEST_LOCATION);
		assertThat(contributor.getKind()).isEqualTo(Kind.INITIAL_IMPORT);
	}

	@Test
	void isActiveWhenPropertiesIsNullReturnsTrue() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(TEST_LOCATION);
		assertThat(contributor.isActive(null)).isTrue();
	}

	@Test
	void isActiveWhenPropertiesIsActiveReturnsTrue() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.activate.on-cloud-platform", "kubernetes");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.isActive(this.activationContext)).isTrue();
	}

	@Test
	void isActiveWhenPropertiesIsNotActiveReturnsFalse() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.activate.on-cloud-platform", "heroku");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.isActive(this.activationContext)).isFalse();
	}

	@Test
	void getLocationReturnsLocation() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataResource resource = mock(ConfigDataResource.class);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(TEST_LOCATION,
				resource, false, configData, 0);
		assertThat(contributor.getResource()).isSameAs(resource);
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
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(null, null,
				false, configData, 0);
		assertThat(contributor.getConfigurationPropertySource()
				.getConfigurationProperty(ConfigurationPropertyName.of("spring")).getValue()).isEqualTo("boot");
	}

	@Test
	void getImportsWhenPropertiesIsNullReturnsEmptyList() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.getImports()).isEmpty();
	}

	@Test
	void getImportsReturnsImports() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "spring,boot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.getImports()).containsExactly(ConfigDataLocation.of("spring"),
				ConfigDataLocation.of("boot"));
	}

	@Test
	void hasUnprocessedImportsWhenNoImportsReturnsFalse() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.hasUnprocessedImports(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isFalse();
	}

	@Test
	void hasUnprocessedImportsWhenHasNoChildrenForPhaseReturnsTrue() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "springboot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.hasUnprocessedImports(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isTrue();
	}

	@Test
	void hasUnprocessedImportsWhenHasChildrenForPhaseReturnsFalse() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "springboot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		ConfigData childConfigData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor childContributor = createBoundContributor(null, childConfigData, 0);
		ConfigDataEnvironmentContributor withChildren = contributor.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(childContributor));
		assertThat(withChildren.hasUnprocessedImports(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isFalse();
		assertThat(withChildren.hasUnprocessedImports(ImportPhase.AFTER_PROFILE_ACTIVATION)).isTrue();
	}

	@Test
	void getChildrenWhenHasNoChildrenReturnsEmptyList() {
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
		assertThat(contributor.getChildren(ImportPhase.AFTER_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void getChildrenWhenHasChildrenReturnsChildren() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "springboot");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(null, configData, 0);
		ConfigData childConfigData = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigDataEnvironmentContributor childContributor = createBoundContributor(null, childConfigData, 0);
		ConfigDataEnvironmentContributor withChildren = contributor.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(childContributor));
		assertThat(withChildren.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).containsExactly(childContributor);
		assertThat(withChildren.getChildren(ImportPhase.AFTER_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void streamReturnsStream() {
		ConfigDataEnvironmentContributor contributor = createBoundContributor("a");
		Stream<String> stream = contributor.stream().map(this::getLocationName);
		assertThat(stream).containsExactly("a");
	}

	@Test
	void iteratorWhenSingleContributorReturnsSingletonIterator() {
		ConfigDataEnvironmentContributor contributor = createBoundContributor("a");
		assertThat(asLocationsList(contributor.iterator())).containsExactly("a");
	}

	@Test
	void iteratorWhenTypicalStructureReturnsCorrectlyOrderedIterator() {
		ConfigDataEnvironmentContributor fileApplication = createBoundContributor("file:application.properties");
		ConfigDataEnvironmentContributor fileProfile = createBoundContributor("file:application-profile.properties");
		ConfigDataEnvironmentContributor fileImports = createBoundContributor("file:./");
		fileImports = fileImports.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(fileApplication));
		fileImports = fileImports.withChildren(ImportPhase.AFTER_PROFILE_ACTIVATION,
				Collections.singletonList(fileProfile));
		ConfigDataEnvironmentContributor classpathApplication = createBoundContributor(
				"classpath:application.properties");
		ConfigDataEnvironmentContributor classpathProfile = createBoundContributor(
				"classpath:application-profile.properties");
		ConfigDataEnvironmentContributor classpathImports = createBoundContributor("classpath:/");
		classpathImports = classpathImports.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Arrays.asList(classpathApplication));
		classpathImports = classpathImports.withChildren(ImportPhase.AFTER_PROFILE_ACTIVATION,
				Arrays.asList(classpathProfile));
		ConfigDataEnvironmentContributor root = createBoundContributor("root");
		root = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Arrays.asList(fileImports, classpathImports));
		assertThat(asLocationsList(root.iterator())).containsExactly("file:application-profile.properties",
				"file:application.properties", "file:./", "classpath:application-profile.properties",
				"classpath:application.properties", "classpath:/", "root");
	}

	@Test
	void withChildrenReturnsNewInstanceWithChildren() {
		ConfigDataEnvironmentContributor root = createBoundContributor("root");
		ConfigDataEnvironmentContributor child = createBoundContributor("child");
		ConfigDataEnvironmentContributor withChildren = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION,
				Collections.singletonList(child));
		assertThat(root.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
		assertThat(withChildren.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).containsExactly(child);
	}

	@Test
	void withChildrenAfterProfileActivationMovesProfileSpecificChildren() {
		ConfigDataEnvironmentContributor root = createBoundContributor("root");
		ConfigDataEnvironmentContributor child1 = createBoundContributor("child1");
		ConfigDataEnvironmentContributor grandchild = createBoundContributor(new TestResource("grandchild"),
				new ConfigData(Collections.singleton(new MockPropertySource()),
						PropertySourceOptions.always(Option.PROFILE_SPECIFIC)),
				0);
		child1 = child1.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.singletonList(grandchild));
		root = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.singletonList(child1));
		ConfigDataEnvironmentContributor child2 = createBoundContributor("child2");
		root = root.withChildren(ImportPhase.AFTER_PROFILE_ACTIVATION, Collections.singletonList(child2));
		assertThat(asLocationsList(root.iterator())).containsExactly("grandchild", "child2", "child1", "root");
	}

	@Test
	void withReplacementReplacesChild() {
		ConfigDataEnvironmentContributor root = createBoundContributor("root");
		ConfigDataEnvironmentContributor child = createBoundContributor("child");
		ConfigDataEnvironmentContributor grandchild = createBoundContributor("grandchild");
		child = child.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.singletonList(grandchild));
		root = root.withChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.singletonList(child));
		ConfigDataEnvironmentContributor updated = createBoundContributor("updated");
		ConfigDataEnvironmentContributor withReplacement = root.withReplacement(grandchild, updated);
		assertThat(asLocationsList(root.iterator())).containsExactly("grandchild", "child", "root");
		assertThat(asLocationsList(withReplacement.iterator())).containsExactly("updated", "child", "root");
	}

	@Test
	void ofCreatesRootContributor() {
		ConfigDataEnvironmentContributor one = createBoundContributor("one");
		ConfigDataEnvironmentContributor two = createBoundContributor("two");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.of(Arrays.asList(one, two));
		assertThat(contributor.getKind()).isEqualTo(Kind.ROOT);
		assertThat(contributor.getResource()).isNull();
		assertThat(contributor.getImports()).isEmpty();
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isNull();
		assertThat(contributor.getConfigurationPropertySource()).isNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).containsExactly(one, two);
	}

	@Test
	void ofInitialImportCreatedInitialImportContributor() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(TEST_LOCATION);
		assertThat(contributor.getKind()).isEqualTo(Kind.INITIAL_IMPORT);
		assertThat(contributor.getResource()).isNull();
		assertThat(contributor.getImports()).containsExactly(TEST_LOCATION);
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
		assertThat(contributor.getResource()).isNull();
		assertThat(contributor.getImports()).isEmpty(); // Properties must not be bound
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void ofUnboundImportCreatesImportedContributor() {
		TestResource resource = new TestResource("test");
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "test");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(TEST_LOCATION,
				resource, false, configData, 0);
		assertThat(contributor.getKind()).isEqualTo(Kind.UNBOUND_IMPORT);
		assertThat(contributor.getResource()).isSameAs(resource);
		assertThat(contributor.getImports()).isEmpty();
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void bindCreatesImportedContributor() {
		TestResource resource = new TestResource("test");
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "test");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource));
		ConfigDataEnvironmentContributor contributor = createBoundContributor(resource, configData, 0);
		assertThat(contributor.getKind()).isEqualTo(Kind.BOUND_IMPORT);
		assertThat(contributor.getResource()).isSameAs(resource);
		assertThat(contributor.getImports()).containsExactly(TEST_LOCATION);
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void bindWhenConfigDataHasIgnoreImportsOptionsCreatesImportedContributorWithoutImports() {
		TestResource resource = new TestResource("test");
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.import", "test");
		ConfigData configData = new ConfigData(Collections.singleton(propertySource), ConfigData.Option.IGNORE_IMPORTS);
		ConfigDataEnvironmentContributor contributor = createBoundContributor(resource, configData, 0);
		assertThat(contributor.getKind()).isEqualTo(Kind.BOUND_IMPORT);
		assertThat(contributor.getResource()).isSameAs(resource);
		assertThat(contributor.getImports()).isEmpty();
		assertThat(contributor.isActive(this.activationContext)).isTrue();
		assertThat(contributor.getPropertySource()).isEqualTo(propertySource);
		assertThat(contributor.getConfigurationPropertySource()).isNotNull();
		assertThat(contributor.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION)).isEmpty();
	}

	@Test
	void bindWhenHasUseLegacyPropertyThrowsException() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.config.use-legacy-processing", "true");
		assertThatExceptionOfType(UseLegacyConfigProcessingException.class).isThrownBy(
				() -> createBoundContributor(null, new ConfigData(Collections.singleton(propertySource)), 0));
	}

	@Test // gh-25029
	void withBoundPropertiesWhenIgnoringImportsAndNothingBound() {
		TestResource resource = new TestResource("a");
		ConfigData configData = new ConfigData(Collections.singleton(new MockPropertySource()), Option.IGNORE_IMPORTS);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(TEST_LOCATION,
				resource, false, configData, 0);
		Binder binder = new Binder(contributor.getConfigurationPropertySource());
		ConfigDataEnvironmentContributor bound = contributor.withBoundProperties(binder);
		assertThat(bound).isNotNull();
	}

	private ConfigDataEnvironmentContributor createBoundContributor(String location) {
		return createBoundContributor(new TestResource(location),
				new ConfigData(Collections.singleton(new MockPropertySource())), 0);
	}

	private ConfigDataEnvironmentContributor createBoundContributor(ConfigDataResource resource, ConfigData configData,
			int propertySourceIndex) {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(TEST_LOCATION,
				resource, false, configData, propertySourceIndex);
		Binder binder = new Binder(contributor.getConfigurationPropertySource());
		return contributor.withBoundProperties(binder);
	}

	private List<String> asLocationsList(Iterator<ConfigDataEnvironmentContributor> iterator) {
		List<String> list = new ArrayList<>();
		iterator.forEachRemaining((contributor) -> list.add(getLocationName(contributor)));
		return list;
	}

	private String getLocationName(ConfigDataEnvironmentContributor contributor) {
		return contributor.getResource().toString();
	}

	static class TestResource extends ConfigDataResource {

		private final String location;

		TestResource(String location) {
			this.location = location;
		}

		@Override
		public String toString() {
			return this.location;
		}

	}

}
