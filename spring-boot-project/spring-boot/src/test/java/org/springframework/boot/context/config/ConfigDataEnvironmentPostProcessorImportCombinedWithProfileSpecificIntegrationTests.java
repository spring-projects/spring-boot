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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigData.Option;
import org.springframework.boot.context.config.ConfigData.Options;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessorIntegrationTests.Config;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConfigDataEnvironmentPostProcessor} config data imports
 * that are combined with profile-specific files.
 *
 * @author Phillip Webb
 */
class ConfigDataEnvironmentPostProcessorImportCombinedWithProfileSpecificIntegrationTests {

	private SpringApplication application;

	@TempDir
	public File temp;

	@BeforeEach
	void setup() {
		this.application = new SpringApplication(Config.class);
		this.application.setWebApplicationType(WebApplicationType.NONE);
	}

	@Test
	void testWithoutProfile() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=configimportwithprofilespecific");
		String value = context.getEnvironment().getProperty("prop");
		assertThat(value).isEqualTo("fromicwps1");
	}

	@Test
	void testWithProfile() {
		ConfigurableApplicationContext context = this.application
				.run("--spring.config.name=configimportwithprofilespecific", "--spring.profiles.active=prod");
		String value = context.getEnvironment().getProperty("prop");
		assertThat(value).isEqualTo("fromicwps2");
	}

	static class LocationResolver implements ConfigDataLocationResolver<Resource> {

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			return location.hasPrefix("icwps:");
		}

		@Override
		public List<Resource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			return Collections.emptyList();
		}

		@Override
		public List<Resource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
				ConfigDataLocation location, Profiles profiles) {
			return Collections.singletonList(new Resource(profiles));
		}

	}

	static class Loader implements ConfigDataLoader<Resource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, Resource resource) throws IOException {
			List<PropertySource<?>> propertySources = new ArrayList<>();
			Map<PropertySource<?>, Options> propertySourceOptions = new HashMap<>();
			propertySources.add(new MapPropertySource("icwps1", Collections.singletonMap("prop", "fromicwps1")));
			if (resource.profiles.isAccepted("prod")) {
				MapPropertySource profileSpecificPropertySource = new MapPropertySource("icwps2",
						Collections.singletonMap("prop", "fromicwps2"));
				propertySources.add(profileSpecificPropertySource);
				propertySourceOptions.put(profileSpecificPropertySource, Options.of(Option.PROFILE_SPECIFIC));
			}
			return new ConfigData(propertySources, propertySourceOptions::get);
		}

	}

	private static class Resource extends ConfigDataResource {

		private final Profiles profiles;

		Resource(Profiles profiles) {
			this.profiles = profiles;
		}

		@Override
		public String toString() {
			return "icwps:";
		}

	}

}
