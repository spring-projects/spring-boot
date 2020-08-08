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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.properties.bind.Binder;
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

	@Mock
	private ConfigDataLocationResolvers resolvers;

	@Mock
	private ConfigDataLoaders loaders;

	@Mock
	private Binder binder;

	@Mock
	private ConfigDataLocationResolverContext locationResolverContext;

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
		List<String> locations = Arrays.asList("test1", "test2");
		TestLocation resolvedLocation1 = new TestLocation();
		TestLocation resolvedLocation2 = new TestLocation();
		List<ConfigDataLocation> resolvedLocations = Arrays.asList(resolvedLocation1, resolvedLocation2);
		ConfigData configData1 = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigData configData2 = new ConfigData(Collections.singleton(new MockPropertySource()));
		given(this.resolvers.resolveAll(this.locationResolverContext, locations, this.profiles))
				.willReturn(resolvedLocations);
		given(this.loaders.load(resolvedLocation1)).willReturn(configData1);
		given(this.loaders.load(resolvedLocation2)).willReturn(configData2);
		ConfigDataImporter importer = new ConfigDataImporter(this.resolvers, this.loaders);
		Collection<ConfigData> loaded = importer
				.resolveAndLoad(this.activationContext, this.locationResolverContext, locations).values();
		assertThat(loaded).containsExactly(configData2, configData1);
	}

	@Test
	void loadImportsWhenAlreadyImportedLocationSkipsLoad() throws Exception {
		List<String> locations1and2 = Arrays.asList("test1", "test2");
		List<String> locations2and3 = Arrays.asList("test2", "test3");
		TestLocation resolvedLocation1 = new TestLocation();
		TestLocation resolvedLocation2 = new TestLocation();
		TestLocation resolvedLocation3 = new TestLocation();
		List<ConfigDataLocation> resolvedLocations1and2 = Arrays.asList(resolvedLocation1, resolvedLocation2);
		List<ConfigDataLocation> resolvedLocations2and3 = Arrays.asList(resolvedLocation2, resolvedLocation3);
		ConfigData configData1 = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigData configData2 = new ConfigData(Collections.singleton(new MockPropertySource()));
		ConfigData configData3 = new ConfigData(Collections.singleton(new MockPropertySource()));
		given(this.resolvers.resolveAll(this.locationResolverContext, locations1and2, this.profiles))
				.willReturn(resolvedLocations1and2);
		given(this.resolvers.resolveAll(this.locationResolverContext, locations2and3, this.profiles))
				.willReturn(resolvedLocations2and3);
		given(this.loaders.load(resolvedLocation1)).willReturn(configData1);
		given(this.loaders.load(resolvedLocation2)).willReturn(configData2);
		given(this.loaders.load(resolvedLocation3)).willReturn(configData3);
		ConfigDataImporter importer = new ConfigDataImporter(this.resolvers, this.loaders);
		Collection<ConfigData> loaded1and2 = importer
				.resolveAndLoad(this.activationContext, this.locationResolverContext, locations1and2).values();
		Collection<ConfigData> loaded2and3 = importer
				.resolveAndLoad(this.activationContext, this.locationResolverContext, locations2and3).values();
		assertThat(loaded1and2).containsExactly(configData2, configData1);
		assertThat(loaded2and3).containsExactly(configData3);
	}

	static class TestLocation extends ConfigDataLocation {

	}

}
