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
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLoaders}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoadersTests {

	private final DeferredLogFactory logFactory = Supplier::get;

	private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private final ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

	@Test
	void createWhenLoaderHasDeferredLogFactoryParameterInjectsDeferredLogFactory() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, DeferredLogFactoryConfigDataLoader.class);
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				springFactoriesLoader);
		assertThat(loaders).extracting("loaders")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.satisfies(this::containsValidDeferredLogFactoryConfigDataLoader);
	}

	private void containsValidDeferredLogFactoryConfigDataLoader(List<?> list) {
		assertThat(list).hasSize(1);
		DeferredLogFactoryConfigDataLoader loader = (DeferredLogFactoryConfigDataLoader) list.get(0);
		assertThat(loader.getLogFactory()).isSameAs(this.logFactory);
	}

	@Test
	void createWhenLoaderHasLogParameterThrowsException() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, LogConfigDataLoader.class);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ConfigDataLoaders(this.logFactory, this.bootstrapContext, springFactoriesLoader))
			.havingCause()
			.isInstanceOf(IllegalArgumentException.class)
			.withMessageContaining("use DeferredLogFactory");
	}

	@Test
	void createWhenLoaderHasBootstrapParametersInjectsBootstrapContext() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, BootstrappingConfigDataLoader.class);
		new ConfigDataLoaders(this.logFactory, this.bootstrapContext, springFactoriesLoader);
		assertThat(this.bootstrapContext.get(String.class)).isEqualTo("boot");
	}

	@Test
	void loadWhenSingleLoaderSupportsLocationReturnsLoadedConfigData() throws Exception {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, TestConfigDataLoader.class);
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				springFactoriesLoader);
		TestConfigDataResource location = new TestConfigDataResource("test");
		ConfigData loaded = loaders.load(this.context, location);
		assertThat(loaded).isNotNull();
		assertThat(getLoader(loaded)).isInstanceOf(TestConfigDataLoader.class);
	}

	@Test
	void loadWhenMultipleLoadersSupportLocationThrowsException() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, AnotherConfigDataLoader.class, TestConfigDataLoader.class);
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				springFactoriesLoader);
		TestConfigDataResource location = new TestConfigDataResource("test");
		assertThatIllegalStateException().isThrownBy(() -> loaders.load(this.context, location))
			.withMessageContaining("Multiple loaders found for resource 'test'");
	}

	@Test
	void loadWhenNoLoaderSupportsLocationThrowsException() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, NonLoadableConfigDataLoader.class);
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				springFactoriesLoader);
		TestConfigDataResource location = new TestConfigDataResource("test");
		assertThatIllegalStateException().isThrownBy(() -> loaders.load(this.context, location))
			.withMessage("No loader found for resource 'test'");
	}

	@Test
	void loadWhenGenericTypeDoesNotMatchSkipsLoader() throws Exception {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.add(ConfigDataLoader.class, OtherConfigDataLoader.class, SpecificConfigDataLoader.class);
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				springFactoriesLoader);
		TestConfigDataResource location = new TestConfigDataResource("test");
		ConfigData loaded = loaders.load(this.context, location);
		assertThat(loaded).isNotNull();
		assertThat(getLoader(loaded)).isInstanceOf(SpecificConfigDataLoader.class);
	}

	private ConfigDataLoader<?> getLoader(ConfigData loaded) {
		ConfigDataLoader<?> result = (ConfigDataLoader<?>) loaded.getPropertySources().get(0).getProperty("loader");
		assertThat(result).isNotNull();
		return result;
	}

	private static ConfigData createConfigData(ConfigDataLoader<?> loader, ConfigDataResource resource) {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("loader", loader);
		propertySource.setProperty("resource", resource);
		List<PropertySource<?>> propertySources = Arrays.asList(propertySource);
		return new ConfigData(propertySources);
	}

	static class TestConfigDataResource extends ConfigDataResource {

		private final String value;

		TestConfigDataResource(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	static class OtherConfigDataResource extends ConfigDataResource {

	}

	static class DeferredLogFactoryConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		private final DeferredLogFactory logFactory;

		DeferredLogFactoryConfigDataLoader(DeferredLogFactory logFactory) {
			assertThat(logFactory).isNotNull();
			this.logFactory = logFactory;
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			throw new AssertionError("Unexpected call");
		}

		DeferredLogFactory getLogFactory() {
			return this.logFactory;
		}

	}

	static class LogConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		final Log logger;

		LogConfigDataLoader(Log logger) {
			this.logger = logger;
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			throw new AssertionError("Unexpected call");
		}

	}

	static class BootstrappingConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		BootstrappingConfigDataLoader(ConfigurableBootstrapContext configurableBootstrapContext,
				BootstrapRegistry bootstrapRegistry, BootstrapContext bootstrapContext) {
			assertThat(configurableBootstrapContext).isNotNull();
			assertThat(bootstrapRegistry).isNotNull();
			assertThat(bootstrapContext).isNotNull();
			assertThat(configurableBootstrapContext).isEqualTo(bootstrapRegistry).isEqualTo(bootstrapContext);
			bootstrapRegistry.register(String.class, InstanceSupplier.of("boot"));
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			throw new AssertionError("Unexpected call");
		}

	}

	static class TestConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			return createConfigData(this, resource);
		}

	}

	static class AnotherConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			return createConfigData(this, resource);
		}

	}

	static class NonLoadableConfigDataLoader extends TestConfigDataLoader {

		@Override
		public boolean isLoadable(ConfigDataLoaderContext context, ConfigDataResource resource) {
			return false;
		}

	}

	static class SpecificConfigDataLoader implements ConfigDataLoader<TestConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource location) throws IOException {
			return createConfigData(this, location);
		}

	}

	static class OtherConfigDataLoader implements ConfigDataLoader<OtherConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, OtherConfigDataResource location) throws IOException {
			return createConfigData(this, location);
		}

	}

}
