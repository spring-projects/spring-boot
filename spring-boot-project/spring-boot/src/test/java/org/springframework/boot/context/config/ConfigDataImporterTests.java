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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ConfigDataImporter}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataImporterTests {

	private DeferredLogFactory logFactory = Supplier::get;

	@Mock
	private ConfigDataLocationResolvers resolvers;

	@Mock
	private ConfigDataLoaders loaders;

	@Mock
	private Binder binder;

	@Mock
	private ConfigDataLocationResolverContext locationResolverContext;

	@Mock
	private ConfigDataLoaderContext loaderContext;

	@Mock
	private ConfigDataActivationContext activationContext;

	@Mock
	private Profiles profiles;

	@BeforeEach
	void setup() {
		given(this.activationContext.getProfiles()).willReturn(this.profiles);
	}

	@Test
	void loadImportsResolvesAndLoadsLocations() throws Exception {
		ConfigDataLocation location1 = ConfigDataLocation.of("test1");
		ConfigDataLocation location2 = ConfigDataLocation.of("test2");
		TestResource resource1 = new TestResource("r1");
		TestResource resource2 = new TestResource("r2");
		ConfigData configData1 = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigData configData2 = new ConfigData(Collections.singleton(new MockPropertySource()));
		given(this.resolvers.resolve(this.locationResolverContext, location1, this.profiles))
				.willReturn(Collections.singletonList(new ConfigDataResolutionResult(location1, resource1, false)));
		given(this.resolvers.resolve(this.locationResolverContext, location2, this.profiles))
				.willReturn(Collections.singletonList(new ConfigDataResolutionResult(location2, resource2, false)));
		given(this.loaders.load(this.loaderContext, resource1)).willReturn(configData1);
		given(this.loaders.load(this.loaderContext, resource2)).willReturn(configData2);
		ConfigDataImporter importer = new ConfigDataImporter(this.logFactory, ConfigDataNotFoundAction.FAIL,
				this.resolvers, this.loaders);
		Collection<ConfigData> loaded = importer.resolveAndLoad(this.activationContext, this.locationResolverContext,
				this.loaderContext, Arrays.asList(location1, location2)).values();
		assertThat(loaded).containsExactly(configData2, configData1);
	}

	@Test
	void loadImportsWhenAlreadyImportedLocationSkipsLoad() throws Exception {
		ConfigDataLocation location1 = ConfigDataLocation.of("test1");
		ConfigDataLocation location2 = ConfigDataLocation.of("test2");
		ConfigDataLocation location3 = ConfigDataLocation.of("test3");
		List<ConfigDataLocation> locations1and2 = Arrays.asList(location1, location2);
		List<ConfigDataLocation> locations2and3 = Arrays.asList(location2, location3);
		TestResource resource1 = new TestResource("r1");
		TestResource resource2 = new TestResource("r2");
		TestResource resource3 = new TestResource("r3");
		ConfigData configData1 = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigData configData2 = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigData configData3 = new ConfigData(Collections.singleton(new MockPropertySource()));
		given(this.resolvers.resolve(this.locationResolverContext, location1, this.profiles))
				.willReturn(Collections.singletonList(new ConfigDataResolutionResult(location1, resource1, false)));
		given(this.resolvers.resolve(this.locationResolverContext, location2, this.profiles))
				.willReturn(Collections.singletonList(new ConfigDataResolutionResult(location2, resource2, false)));
		given(this.resolvers.resolve(this.locationResolverContext, location3, this.profiles))
				.willReturn(Collections.singletonList(new ConfigDataResolutionResult(location3, resource3, false)));
		given(this.loaders.load(this.loaderContext, resource1)).willReturn(configData1);
		given(this.loaders.load(this.loaderContext, resource2)).willReturn(configData2);
		given(this.loaders.load(this.loaderContext, resource3)).willReturn(configData3);
		ConfigDataImporter importer = new ConfigDataImporter(this.logFactory, ConfigDataNotFoundAction.FAIL,
				this.resolvers, this.loaders);
		Collection<ConfigData> loaded1and2 = importer.resolveAndLoad(this.activationContext,
				this.locationResolverContext, this.loaderContext, locations1and2).values();
		Collection<ConfigData> loaded2and3 = importer.resolveAndLoad(this.activationContext,
				this.locationResolverContext, this.loaderContext, locations2and3).values();
		assertThat(loaded1and2).containsExactly(configData2, configData1);
		assertThat(loaded2and3).containsExactly(configData3);
	}

	static class TestResource extends ConfigDataResource {

		private final String name;

		TestResource(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
