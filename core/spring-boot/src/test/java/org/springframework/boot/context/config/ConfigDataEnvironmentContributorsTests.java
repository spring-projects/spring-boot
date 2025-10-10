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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributors.BinderOption;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironmentContributors}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataEnvironmentContributorsTests {

	private static final ConfigDataLocation LOCATION_1 = ConfigDataLocation.of("location1");

	private static final ConfigDataLocation LOCATION_2 = ConfigDataLocation.of("location2");

	private final DeferredLogFactory logFactory = Supplier::get;

	private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

	private ConfigDataImporter importer;

	private ConfigDataActivationContext activationContext;

	@BeforeEach
	void setup() {
		MockEnvironment environment = new MockEnvironment();
		Binder binder = Binder.get(environment);
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				binder, new DefaultResourceLoader(getClass().getClassLoader()),
				SpringFactoriesLoader.forDefaultResourceLocation(getClass().getClassLoader()));
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				SpringFactoriesLoader.forDefaultResourceLocation());
		this.importer = new ConfigDataImporter(this.logFactory, ConfigDataNotFoundAction.FAIL, resolvers, loaders);
		this.activationContext = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, null);
	}

	@Test
	void createCreatesWithInitialContributors() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Iterator<ConfigDataEnvironmentContributor> iterator = contributors.iterator();
		assertThat(iterator.next()).isSameAs(contributor);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
	}

	@Test
	void withProcessedImportsWhenHasNoUnprocessedImportsReturnsSameInstance() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
			.ofExisting(new MockPropertySource(), this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
				this.activationContext);
		assertThat(withProcessedImports).isSameAs(contributors);
	}

	@Test
	void withProcessedImportsResolvesAndLoads() {
		this.importer = mock(ConfigDataImporter.class);
		List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
		MockPropertySource propertySource = new MockPropertySource();
		Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
		imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
				new ConfigData(List.of(propertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
			.willReturn(imported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
				this.activationContext);
		Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
		assertThat(iterator.next().getPropertySource()).isSameAs(propertySource);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.INITIAL_IMPORT);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void withProcessedImportsResolvesAndLoadsChainedImports() {
		this.importer = mock(ConfigDataImporter.class);
		List<ConfigDataLocation> initialLocations = Collections.singletonList(LOCATION_1);
		MockPropertySource initialPropertySource = new MockPropertySource();
		initialPropertySource.setProperty("spring.config.import", "location2");
		Map<ConfigDataResolutionResult, ConfigData> initialImported = new LinkedHashMap<>();
		initialImported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
				new ConfigData(List.of(initialPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(initialLocations)))
			.willReturn(initialImported);
		List<ConfigDataLocation> secondLocations = Collections.singletonList(LOCATION_2);
		MockPropertySource secondPropertySource = new MockPropertySource();
		Map<ConfigDataResolutionResult, ConfigData> secondImported = new LinkedHashMap<>();
		secondImported.put(new ConfigDataResolutionResult(LOCATION_2, new TestConfigDataResource("b"), false),
				new ConfigData(List.of(secondPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(secondLocations)))
			.willReturn(secondImported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
				this.activationContext);
		Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
		assertThat(iterator.next().getPropertySource()).isSameAs(secondPropertySource);
		assertThat(iterator.next().getPropertySource()).isSameAs(initialPropertySource);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.INITIAL_IMPORT);
		assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void withProcessedImportsProvidesLocationResolverContextWithAccessToBinder() {
		MockPropertySource existingPropertySource = new MockPropertySource();
		existingPropertySource.setProperty("test", "springboot");
		ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
			.ofExisting(existingPropertySource, this.conversionService);
		this.importer = mock(ConfigDataImporter.class);
		List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
		MockPropertySource propertySource = new MockPropertySource();
		Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
		imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
				new ConfigData(List.of(propertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
			.willReturn(imported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(existingContributor, contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		contributors.withProcessedImports(this.importer, this.activationContext);
		then(this.importer).should()
			.resolveAndLoad(any(),
					assertArg((context) -> assertThat(context.getBinder().bind("test", String.class).get())
						.isEqualTo("springboot")),
					any(), any());
	}

	@Test
	void withProcessedImportsProvidesLocationResolverContextWithAccessToParent() {
		this.importer = mock(ConfigDataImporter.class);
		List<ConfigDataLocation> initialLocations = Collections.singletonList(LOCATION_1);
		MockPropertySource initialPropertySource = new MockPropertySource();
		initialPropertySource.setProperty("spring.config.import", "location2");
		Map<ConfigDataResolutionResult, ConfigData> initialImported = new LinkedHashMap<>();
		initialImported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
				new ConfigData(List.of(initialPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(initialLocations)))
			.willReturn(initialImported);
		List<ConfigDataLocation> secondLocations = Collections.singletonList(LOCATION_2);
		MockPropertySource secondPropertySource = new MockPropertySource();
		Map<ConfigDataResolutionResult, ConfigData> secondImported = new LinkedHashMap<>();
		secondImported.put(new ConfigDataResolutionResult(LOCATION_2, new TestConfigDataResource("b"), false),
				new ConfigData(List.of(secondPropertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(secondLocations)))
			.willReturn(secondImported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		ArgumentCaptor<ConfigDataLocationResolverContext> locationResolverContext = ArgumentCaptor
			.forClass(ConfigDataLocationResolverContext.class);
		contributors.withProcessedImports(this.importer, this.activationContext);
		then(this.importer).should()
			.resolveAndLoad(any(), locationResolverContext.capture(), any(), eq(secondLocations));
		ConfigDataLocationResolverContext context = locationResolverContext.getValue();
		assertThat(context.getParent()).hasToString("a");
	}

	@Test
	void withProcessedImportsProvidesLocationResolverContextWithAccessToBootstrapRegistry() {
		MockPropertySource existingPropertySource = new MockPropertySource();
		existingPropertySource.setProperty("test", "springboot");
		ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
			.ofExisting(existingPropertySource, this.conversionService);
		this.importer = mock(ConfigDataImporter.class);
		List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
		MockPropertySource propertySource = new MockPropertySource();
		Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
		imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
				new ConfigData(List.of(propertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
			.willReturn(imported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(existingContributor, contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		contributors.withProcessedImports(this.importer, this.activationContext);
		then(this.importer).should()
			.resolveAndLoad(any(),
					assertArg((context) -> assertThat(context.getBootstrapContext()).isSameAs(this.bootstrapContext)),
					any(), any());
	}

	@Test
	void withProcessedImportsProvidesLoaderContextWithAccessToBootstrapRegistry() {
		MockPropertySource existingPropertySource = new MockPropertySource();
		existingPropertySource.setProperty("test", "springboot");
		ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
			.ofExisting(existingPropertySource, this.conversionService);
		this.importer = mock(ConfigDataImporter.class);
		List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
		MockPropertySource propertySource = new MockPropertySource();
		Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
		imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
				new ConfigData(List.of(propertySource)));
		given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
			.willReturn(imported);
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(existingContributor, contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		contributors.withProcessedImports(this.importer, this.activationContext);
		then(this.importer).should()
			.resolveAndLoad(any(), any(),
					assertArg((context) -> assertThat(context.getBootstrapContext()).isSameAs(this.bootstrapContext)),
					any());
	}

	@Test
	void getBinderProvidesBinder() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("test", "springboot");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
	}

	@Test
	void getBinderWhenHasMultipleSourcesPicksFirst() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
		ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("one");
	}

	@Test
	void getBinderWhenHasInactiveIgnoresInactive() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
		ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
	}

	@Test
	void getBinderWhenHasPlaceholderResolvesPlaceholder() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("test", "${other}");
		propertySource.setProperty("other", "springboot");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource,
				this.conversionService);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, List.of(contributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
	}

	@Test
	void getBinderWhenHasPlaceholderAndInactiveResolvesPlaceholderOnlyFromActive() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("other", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("other", "two");
		secondPropertySource.setProperty("test", "${other}");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
		ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext);
		assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
	}

	@Test
	void getBinderWhenFailOnBindToInactiveSourceWithFirstInactiveThrowsException() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
		ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
			.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
	}

	@Test
	void getBinderWhenFailOnBindToInactiveSourceWithLastInactiveThrowsException() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("test", "one");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("spring.config.activate.on-profile", "production");
		secondPropertySource.setProperty("test", "two");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
		ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
			.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
	}

	@Test
	void getBinderWhenFailOnBindToInactiveSourceWithResolveToInactiveThrowsException() {
		MockPropertySource firstPropertySource = new MockPropertySource();
		firstPropertySource.setProperty("other", "one");
		firstPropertySource.setProperty("spring.config.activate.on-profile", "production");
		MockPropertySource secondPropertySource = new MockPropertySource();
		secondPropertySource.setProperty("test", "${other}");
		secondPropertySource.setProperty("other", "one");
		ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
		ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
		ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
		ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.logFactory,
				this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
			.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
	}

	private ConfigDataEnvironmentContributor createBoundImportContributor(ConfigData configData,
			int propertySourceIndex) {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(null, null,
				false, configData, propertySourceIndex, this.conversionService,
				ConfigDataEnvironmentUpdateListener.NONE);
		return contributor.withBoundProperties(Collections.singleton(contributor), null);
	}

	private static class TestConfigDataResource extends ConfigDataResource {

		private final String value;

		TestConfigDataResource(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

}
