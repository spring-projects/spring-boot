/*
 * Copyright 2012-present the original author or authors.
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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.MockApplicationEnvironment;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.config.TestConfigDataEnvironmentUpdateListener.AddedPropertySource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentTests {

	private final DeferredLogFactory logFactory = Supplier::get;

	private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private final MockApplicationEnvironment environment = new MockApplicationEnvironment();

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final Collection<String> additionalProfiles = Collections.emptyList();

	private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

	@Test
	void createExposesEnvironmentBinderToConfigDataLocationResolvers() {
		this.environment.setProperty("spring", "boot");
		TestConfigDataEnvironment configDataEnvironment = new TestConfigDataEnvironment(this.logFactory,
				this.bootstrapContext, this.environment, this.resourceLoader, this.additionalProfiles, null);
		Binder binder = configDataEnvironment.getConfigDataLocationResolversBinder();
		assertThat(binder).isNotNull();
		assertThat(binder.bind("spring", String.class).get()).isEqualTo("boot");
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
		List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors()
			.getRoot()
			.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
		Object[] wrapped = children.stream()
			.filter((child) -> child.getKind() == Kind.EXISTING)
			.map(ConfigDataEnvironmentContributor::getPropertySource)
			.toArray();
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
		List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors()
			.getRoot()
			.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
		Object[] wrapped = children.stream()
			.filter((child) -> child.getKind() == Kind.EXISTING)
			.map(ConfigDataEnvironmentContributor::getPropertySource)
			.toArray();
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
		List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors()
			.getRoot()
			.getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
		Object[] imports = children.stream()
			.filter((child) -> child.getKind() == Kind.INITIAL_IMPORT)
			.map(ConfigDataEnvironmentContributor::getImports)
			.map(Object::toString)
			.toArray();
		assertThat(imports).containsExactly("[i2]", "[i1]", "[a2]", "[a1]", "[l2]", "[l1]");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring=boot")
	void processAndApplyAddsImportedSourceToEnvironment() {
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring=boot
			#---
			spring.config.activate.on-profile=missing
			other=value
			No newline at end of file
			""")
	void processAndApplyOnlyAddsActiveContributors() {
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
		List<PropertySource<?>> sources = this.environment.getPropertySources().stream().toList();
		assertThat(sources.get(sources.size() - 1)).isSameAs(defaultPropertySource);
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.default=one,two,three")
	void processAndApplySetsDefaultProfiles(TestInfo info) {
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getDefaultProfiles()).containsExactly("one", "two", "three");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.active=one,two,three")
	void processAndApplySetsActiveProfiles() {
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("one", "two", "three");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=one,two,three
			spring.profiles.group.one=four,five
			""")
	void processAndApplySetsActiveProfilesAndProfileGroups() {
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("one", "four", "five", "two", "three");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.active=test")
	void processAndApplyDoesNotSetProfilesFromIgnoreProfilesContributors(TestInfo info) {
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
						mock(ConfigDataResource.class), false, data, 0,
						ConfigDataEnvironmentTests.this.conversionService, ConfigDataEnvironmentUpdateListener.NONE));
				return super.createContributors(contributors);
			}

		};
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	@ParameterizedTest
	@ValueSource(strings = { "include", "include[0]" })
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
						mock(ConfigDataResource.class), false, data, 0,
						ConfigDataEnvironmentTests.this.conversionService, ConfigDataEnvironmentUpdateListener.NONE));
				return super.createContributors(contributors);
			}

		};
		assertThatExceptionOfType(InactiveConfigDataAccessException.class)
			.isThrownBy(configDataEnvironment::processAndApply);
	}

	@ParameterizedTest
	@ValueSource(strings = { "spring.profiles.include", "spring.profiles.include[0]" })
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
						mock(ConfigDataResource.class), false, data, 0,
						ConfigDataEnvironmentTests.this.conversionService, ConfigDataEnvironmentUpdateListener.NONE));
				return super.createContributors(contributors);
			}

		};
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).containsExactly("included");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring=boot")
	void processAndApplyDoesNotSetProfilesFromIgnoreProfilesContributorsWhenNoProfilesActive(TestInfo info) {
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
						mock(ConfigDataResource.class), false, data, 0,
						ConfigDataEnvironmentTests.this.conversionService, ConfigDataEnvironmentUpdateListener.NONE));
				return super.createContributors(contributors);
			}

		};
		configDataEnvironment.processAndApply();
		assertThat(this.environment.getActiveProfiles()).isEmpty();
		assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
	}

	@Test
	void processAndApplyWhenHasInvalidPropertyThrowsException() {
		this.environment.setProperty("spring.profiles", "a");
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, null);
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
			.isThrownBy(configDataEnvironment::processAndApply);
	}

	@Test
	@WithResource(name = "custom/config.properties", content = "spring=boot")
	void processAndApplyWhenHasListenerCallsOnPropertySourceAdded(TestInfo info) {
		this.environment.setProperty("spring.config.location", "classpath:custom/config.properties");
		TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, listener);
		configDataEnvironment.processAndApply();
		assertThat(listener.getAddedPropertySources()).hasSize(1);
		AddedPropertySource addedPropertySource = listener.getAddedPropertySources().get(0);
		assertThat(addedPropertySource.getPropertySource().getProperty("spring")).isEqualTo("boot");
		assertThat(addedPropertySource.getLocation()).hasToString("classpath:custom/config.properties");
		ConfigDataResource resource = addedPropertySource.getResource();
		assertThat(resource).isNotNull();
		assertThat(resource.toString()).contains("class path resource").contains("custom/config.properties");
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.profiles.active=one,two,three")
	void processAndApplyWhenHasListenerCallsOnSetProfiles(TestInfo info) {
		TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
		ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.logFactory, this.bootstrapContext,
				this.environment, this.resourceLoader, this.additionalProfiles, listener);
		configDataEnvironment.processAndApply();
		Profiles profiles = listener.getProfiles();
		assertThat(profiles).isNotNull();
		assertThat(profiles.getActive()).containsExactly("one", "two", "three");
	}

	@Test
	@SuppressWarnings("rawtypes")
	@WithResource(name = "separate-class-loader-spring.factories", content = """
			org.springframework.boot.context.config.ConfigDataLoader=\
			org.springframework.boot.context.config.ConfigDataEnvironmentTests$SeparateClassLoaderConfigDataLoader
			""")
	void configDataLoadersAreLoadedUsingClassLoaderFromResourceLoader() {
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		ClassLoader classLoader = new ClassLoader(Thread.currentThread().getContextClassLoader()) {

			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION.equals(name)) {
					return super.getResources("separate-class-loader-spring.factories");
				}
				return super.getResources(name);
			}

		};
		given(resourceLoader.getClassLoader()).willReturn(classLoader);
		TestConfigDataEnvironment configDataEnvironment = new TestConfigDataEnvironment(this.logFactory,
				this.bootstrapContext, this.environment, resourceLoader, this.additionalProfiles, null);
		assertThat(configDataEnvironment).extracting("loaders.loaders")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.extracting((item) -> (Class) item.getClass())
			.containsOnly(SeparateClassLoaderConfigDataLoader.class);
	}

	private String getConfigLocation(TestInfo info) {
		return "optional:classpath:" + info.getTestClass().get().getName().replace('.', '/') + "-"
				+ info.getTestMethod().get().getName() + ".properties";
	}

	static class TestConfigDataEnvironment extends ConfigDataEnvironment {

		private @Nullable Binder configDataLocationResolversBinder;

		TestConfigDataEnvironment(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
				ConfigurableEnvironment environment, ResourceLoader resourceLoader,
				Collection<String> additionalProfiles,
				@Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
			super(logFactory, bootstrapContext, environment, resourceLoader, additionalProfiles,
					environmentUpdateListener);
		}

		@Override
		protected ConfigDataLocationResolvers createConfigDataLocationResolvers(DeferredLogFactory logFactory,
				ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
			this.configDataLocationResolversBinder = binder;
			return super.createConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
		}

		@Nullable Binder getConfigDataLocationResolversBinder() {
			return this.configDataLocationResolversBinder;
		}

	}

	static class SeparateClassLoaderConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		@Override
		public @Nullable ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource)
				throws IOException, ConfigDataResourceNotFoundException {
			return null;
		}

	}

}
