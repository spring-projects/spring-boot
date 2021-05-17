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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.config.TestConfigDataEnvironmentUpdateListener.AddedPropertySource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentTests {

	private DeferredLogFactory logFactory = Supplier::get;

	private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private MockEnvironment environment = new MockEnvironment();

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private Collection<String> additionalProfiles = Collections.emptyList();

	@Test
	void createWhenUseLegacyPropertyInEnvironmentThrowsException() {
		this.environment.setProperty("spring.config.use-legacy-processing", "true");
		assertThatExceptionOfType(UseLegacyConfigProcessingException.class)
				.isThrownBy(() -> new ConfigDataEnvironment(this.logFactory, this.bootstrapContext, this.environment,
						this.resourceLoader, this.additionalProfiles, null));
	}

	@Test
	void createExposesEnvironmentBinderToConfigDataLocationResolvers() {
		this.environment.setProperty("spring", "boot");
		TestConfigDataEnvironment configDataEnvironment = new TestConfigDataEnvironment(this.logFactory,
				this.bootstrapContext, this.environment, this.resourceLoader, this.additionalProfiles, null);
		assertThat(configDataEnvironment.getConfigDataLocationResolversBinder().bind("spring", String.class).get())
				.isEqualTo("boot");
	}

	@Test
	void createCreatesContributorsBasedOnExistingSources() {
		MockPropertySource propertySource1 = new MockPropertySource("p1");
		MockPropertySource propertySource2 = new MockPropertySource("p2");
		MockPropertySource propertySource3 = new MockPropertySource("p3");
		this.environment.getPropertySources().addLast(propertySource1);
		this.environment.getPropertySources().addLast(propertySource2);
		this.environment.getPropertySources().addLast(propertySource3);
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors().getRoot()
				.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
		Object[] wrapped = children.stream().filter((child) -> child.getKind() == Kind.EXISTING)
				.map(ConfigDataEnvironmentContributor::getPropertySource).toArray();
		assertThat(wrapped[1]).isEqualTo(propertySource1);
		assertThat(wrapped[2]).isEqualTo(propertySource2);
		assertThat(wrapped[3]).isEqualTo(propertySource3);
	}

	@Test
	void createWhenHasDefaultPropertySourceMovesItToLastContributor() {
		MockPropertySource defaultPropertySource = new MockPropertySource("defaultProperties");
		MockPropertySource propertySource1 = new MockPropertySource("p2");
		MockPropertySource propertySource2 = new MockPropertySource("p3");
		this.environment.getPropertySources().addLast(defaultPropertySource);
		this.environment.getPropertySources().addLast(propertySource1);
		this.environment.getPropertySources().addLast(propertySource2);
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors().getRoot()
				.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
		Object[] wrapped = children.stream().filter((child) -> child.getKind() == Kind.EXISTING)
				.map(ConfigDataEnvironmentContributor::getPropertySource).toArray();
		assertThat(wrapped[1]).isEqualTo(propertySource1);
		assertThat(wrapped[2]).isEqualTo(propertySource2);
		assertThat(wrapped[3]).isEqualTo(defaultPropertySource);
	}

	@Test
	void createCreatesInitialImportContributorsInCorrectOrder() {
		this.environment.setProperty("spring.config.location", "l1,l2");
		this.environment.setProperty("spring.config.additional-location", "a1,a2");
		this.environment.setProperty("spring.config.import", "i1,i2");
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors().getRoot()
				.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
		Object[] imports = children.stream().filter((child) -> child.getKind() == Kind.INITIAL_IMPORT)
				.map(ConfigDataEnvironmentContributor::getImports).map(Object::toString).toArray();
		assertThat(imports).containsExactly("[i2]", "[i1]", "[a2]", "[a1]", "[l2]", "[l1]");
	}

	@Test
	void processAndApplyAddsImportedSourceToEnvironment(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
	}

	@Test
	void processAndApplyOnlyAddsActiveContributors(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
		assertThat(this.environment.getProperty("other")).isNull();
	}

	@Test
	void processAndApplyMovesDefaultPropertySourceToLast(TestInfo info) {
		MockPropertySource defaultPropertySource = new MockPropertySource("defaultProperties");
		this.environment.getPropertySources().addFirst(defaultPropertySource);
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		List<PropertySource<?>> sources = this.environment.getPropertySources().stream().collect(Collectors.toList());
		assertThat(sources.get(sources.size() - 1)).isSameAs(defaultPropertySource);
	}

	@Test
	void processAndApplySetsDefaultProfiles(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getDefaultProfiles()).containsExactly("one", "two", "three");
	}

	@Test
	void processAndApplySetsActiveProfiles(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("one", "two", "three");
	}

	@Test
	void processAndApplySetsActiveProfilesAndProfileGroups(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("one", "four", "five", "two", "three");
	}

	@Test
	void processAndApplyDoesNotSetProfilesFromIgnoreProfilesContributors(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null) {

			@Override
			protected ConfigDataEnvironmentContributors createContributors(
					List<ConfigDataEnvironmentContributor> contributors) {
				Map<String, Object> source = new LinkedHashMap<>();
				source.put("spring.profiles.active", "ignore1");
				source.put("spring.profiles.include", "ignore2");
				ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)),
						ConfigData.Option.IGNORE_PROFILES);
				contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
						mock(ConfigDataResource.class), false, data, 0));
				return super.createContributors(contributors);
			}

		};
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	@ParameterizedTest
	@CsvSource({ "include", "include[0]" })
	void processAndApplyWhenHasProfileIncludeInProfileSpecificDocumentThrowsException(String property, TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null) {

			@Override
			protected ConfigDataEnvironmentContributors createContributors(
					List<ConfigDataEnvironmentContributor> contributors) {
				Map<String, Object> source = new LinkedHashMap<>();
				source.put("spring.config.activate.on-profile", "activate");
				source.put("spring.profiles." + property, "include");
				ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)));
				contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
						mock(ConfigDataResource.class), false, data, 0));
				return super.createContributors(contributors);
			}

		};
		assertThatExceptionOfType(InactiveConfigDataAccessException.class)
				.isThrownBy(configDataEnvironment::processAndApply);
	}

	@ParameterizedTest
	@CsvSource({ "spring.profiles.include", "spring.profiles.include[0]" })
	void processAndApplyIncludesProfilesFromSpringProfilesInclude(String property, TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null) {

			@Override
			protected ConfigDataEnvironmentContributors createContributors(
					List<ConfigDataEnvironmentContributor> contributors) {
				Map<String, Object> source = new LinkedHashMap<>();
				source.put(property, "included");
				ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)));
				contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
						mock(ConfigDataResource.class), false, data, 0));
				return super.createContributors(contributors);
			}

		};
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("included");
	}

	@Test
	@Disabled("Disabled until spring.profiles support is dropped")
	void processAndApplyWhenHasInvalidPropertyThrowsException() {
		this.environment.setProperty("spring.profile", "a");
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
				.isThrownBy(() -> configDataEnvironment.processAndApply());
	}

	@Test
	void processAndApplyWhenHasListenerCallsOnPropertySourceAdded(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, listener);
		configDataEnvironment.processAndApply();
		assertThat(listener.getAddedPropertySources()).hasSize(1);
		AddedPropertySource addedPropertySource = listener.getAddedPropertySources().get(0);
		assertThat(addedPropertySource.getPropertySource().getProperty("spring")).isEqualTo("boot");
		assertThat(addedPropertySource.getLocation().toString()).isEqualTo(getConfigLocation(info));
		assertThat(addedPropertySource.getResource().toString()).contains("class path resource")
				.contains(info.getTestMethod().get().getName());
	}

	@Test
	void processAndApplyWhenHasListenerCallsOnSetProfiles(TestInfo info) {
		this.environment.setProperty("spring.config.location", getConfigLocation(info));
		TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, listener);
		configDataEnvironment.processAndApply();
		assertThat(listener.getProfiles().getActive()).containsExactly("one", "two", "three");
	}

	private String getConfigLocation(TestInfo info) {
		return "optional:classpath:" + info.getTestClass().get().getName().replace('.', '/') + "-"
				+ info.getTestMethod().get().getName() + ".properties";
	}

	static class TestConfigDataEnvironment extends ConfigDataEnvironment {

		private Binder configDataLocationResolversBinder;

		TestConfigDataEnvironment(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
				ConfigurableEnvironment environment, ResourceLoader resourceLoader,
				Collection<String> additionalProfiles, ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
			super(logFactory, bootstrapContext, environment, resourceLoader, additionalProfiles,
					environmentUpdateListener);
		}

		@Override
		protected ConfigDataLocationResolvers createConfigDataLocationResolvers(DeferredLogFactory logFactory,
				ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
			this.configDataLocationResolversBinder = binder;
			return super.createConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
		}

		Binder getConfigDataLocationResolversBinder() {
			return this.configDataLocationResolversBinder;
		}

	}

}
