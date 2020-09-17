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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLoaders}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoadersTests {

	private DeferredLogFactory logFactory = Supplier::get;

	private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

	@Test
	void createWhenLoaderHasLogParameterInjectsLog() {
		new ConfigDataLoaders(this.logFactory, this.bootstrapContext, ConfigDataLocationNotFoundAction.FAIL,
				Arrays.asList(LoggingConfigDataLoader.class.getName()));
	}

	@Test
	void createWhenLoaderHasBootstrapParametersInjectsBootstrapContext() {
		new ConfigDataLoaders(this.logFactory, this.bootstrapContext, ConfigDataLocationNotFoundAction.FAIL,
				Arrays.asList(BootstrappingConfigDataLoader.class.getName()));
		assertThat(this.bootstrapContext.get(String.class)).isEqualTo("boot");
	}

	@Test
	void loadWhenSingleLoaderSupportsLocationReturnsLoadedConfigData() throws Exception {
		TestConfigDataLocation location = new TestConfigDataLocation("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				ConfigDataLocationNotFoundAction.FAIL, Arrays.asList(TestConfigDataLoader.class.getName()));
		ConfigData loaded = loaders.load(this.context, location);
		assertThat(getLoader(loaded)).isInstanceOf(TestConfigDataLoader.class);
	}

	@Test
	void loadWhenMultipleLoadersSupportLocationThrowsException() throws Exception {
		TestConfigDataLocation location = new TestConfigDataLocation("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				ConfigDataLocationNotFoundAction.FAIL,
				Arrays.asList(LoggingConfigDataLoader.class.getName(), TestConfigDataLoader.class.getName()));
		assertThatIllegalStateException().isThrownBy(() -> loaders.load(this.context, location))
				.withMessageContaining("Multiple loaders found for location test");
	}

	@Test
	void loadWhenNoLoaderSupportsLocationThrowsException() {
		TestConfigDataLocation location = new TestConfigDataLocation("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				ConfigDataLocationNotFoundAction.FAIL, Arrays.asList(NonLoadableConfigDataLoader.class.getName()));
		assertThatIllegalStateException().isThrownBy(() -> loaders.load(this.context, location))
				.withMessage("No loader found for location 'test'");
	}

	@Test
	void loadWhenGenericTypeDoesNotMatchSkipsLoader() throws Exception {
		TestConfigDataLocation location = new TestConfigDataLocation("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext,
				ConfigDataLocationNotFoundAction.FAIL,
				Arrays.asList(OtherConfigDataLoader.class.getName(), SpecificConfigDataLoader.class.getName()));
		ConfigData loaded = loaders.load(this.context, location);
		assertThat(getLoader(loaded)).isInstanceOf(SpecificConfigDataLoader.class);
	}

	private ConfigDataLoader<?> getLoader(ConfigData loaded) {
		return (ConfigDataLoader<?>) loaded.getPropertySources().get(0).getProperty("loader");
	}

	private static ConfigData createConfigData(ConfigDataLoader<?> loader, ConfigDataLocation location) {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("loader", loader);
		propertySource.setProperty("location", location);
		List<PropertySource<?>> propertySources = Arrays.asList(propertySource);
		return new ConfigData(propertySources);

	}

	static class TestConfigDataLocation extends ConfigDataLocation {

		private final String value;

		TestConfigDataLocation(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	static class OtherConfigDataLocation extends ConfigDataLocation {

	}

	static class LoggingConfigDataLoader implements ConfigDataLoader<ConfigDataLocation> {

		LoggingConfigDataLoader(Log log) {
			assertThat(log).isNotNull();
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataLocation location) throws IOException {
			throw new AssertionError("Unexpected call");
		}

	}

	static class BootstrappingConfigDataLoader implements ConfigDataLoader<ConfigDataLocation> {

		BootstrappingConfigDataLoader(ConfigurableBootstrapContext configurableBootstrapContext,
				BootstrapRegistry bootstrapRegistry, BootstrapContext bootstrapContext) {
			assertThat(configurableBootstrapContext).isNotNull();
			assertThat(bootstrapRegistry).isNotNull();
			assertThat(bootstrapContext).isNotNull();
			assertThat(configurableBootstrapContext).isEqualTo(bootstrapRegistry).isEqualTo(bootstrapContext);
			bootstrapRegistry.register(String.class, InstanceSupplier.of("boot"));
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataLocation location) throws IOException {
			throw new AssertionError("Unexpected call");
		}

	}

	static class TestConfigDataLoader implements ConfigDataLoader<ConfigDataLocation> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataLocation location) throws IOException {
			return createConfigData(this, location);
		}

	}

	static class NonLoadableConfigDataLoader extends TestConfigDataLoader {

		@Override
		public boolean isLoadable(ConfigDataLoaderContext context, ConfigDataLocation location) {
			return false;
		}

	}

	static class SpecificConfigDataLoader implements ConfigDataLoader<TestConfigDataLocation> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataLocation location) throws IOException {
			return createConfigData(this, location);
		}

	}

	static class OtherConfigDataLoader implements ConfigDataLoader<OtherConfigDataLocation> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, OtherConfigDataLocation location) throws IOException {
			return createConfigData(this, location);
		}

	}

}
