/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigFileApplicationListener} handling of negated profiles in yaml
 * configuration files.
 *
 * @author Madhura Bhave
 */
class ConfigFileApplicationListenerYamlProfileNegationTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void yamlProfileNegationDefaultProfile() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=profilenegation";
		this.context = application.run(configName);
		assertVersionProperty(this.context, "NOT A");
	}

	@Test
	void yamlProfileNegationWithActiveProfile() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=profilenegation";
		this.context = application.run(configName, "--spring.profiles.active=C,A");
		assertVersionProperty(this.context, null, "C", "A");
	}

	@Test
	void yamlProfileNegationLocalActiveProfiles() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=profilenegation-local-active-profiles";
		this.context = application.run(configName);
		assertVersionProperty(this.context, "NOT A", "B");
	}

	@Test
	void yamlProfileNegationOverrideLocalActiveProfiles() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=profilenegation-local-active-profiles";
		this.context = application.run(configName, "--spring.profiles.active=C,A");
		assertVersionProperty(this.context, null, "C", "A");
	}

	@Test
	void yamlProfileNegationWithProfileSpecificFile() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=profilenegation";
		this.context = application.run(configName, "--spring.profiles.active=C,B");
		assertVersionProperty(this.context, "NOT A", "C", "B");
	}

	@Test
	void yamlProfileCascading() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=cascadingprofiles";
		this.context = application.run(configName);
		assertVersionProperty(this.context, "D", "A", "C", "E", "B", "D");
		assertThat(this.context.getEnvironment().getProperty("not-a")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-b")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-c")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-d")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-e")).isNull();
	}

	@Test
	void yamlProfileCascadingOverrideProfilesA() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=cascadingprofiles";
		this.context = application.run(configName, "--spring.profiles.active=A");
		assertVersionProperty(this.context, "E", "A", "C", "E");
		assertThat(this.context.getEnvironment().getProperty("not-a")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-b")).isEqualTo("true");
		assertThat(this.context.getEnvironment().getProperty("not-c")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-d")).isEqualTo("true");
		assertThat(this.context.getEnvironment().getProperty("not-e")).isNull();
	}

	@Test
	void yamlProfileCascadingMultipleActiveProfilesViaPropertiesShouldPreserveOrder() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=cascadingprofiles";
		this.context = application.run(configName, "--spring.profiles.active=A,B");
		assertVersionProperty(this.context, "D", "A", "C", "E", "B", "D");
		assertThat(this.context.getEnvironment().getProperty("not-a")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-b")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-c")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-d")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-e")).isNull();
	}

	@Test
	void yamlProfileCascadingOverrideProfilesB() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=cascadingprofiles";
		this.context = application.run(configName, "--spring.profiles.active=B");
		assertVersionProperty(this.context, "E", "B", "D", "E");
		assertThat(this.context.getEnvironment().getProperty("not-a")).isEqualTo("true");
		assertThat(this.context.getEnvironment().getProperty("not-b")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-c")).isEqualTo("true");
		assertThat(this.context.getEnvironment().getProperty("not-d")).isNull();
		assertThat(this.context.getEnvironment().getProperty("not-e")).isNull();
	}

	private void assertVersionProperty(ConfigurableApplicationContext context, String expectedVersion,
			String... expectedActiveProfiles) {
		assertThat(context.getEnvironment().getActiveProfiles()).isEqualTo(expectedActiveProfiles);
		assertThat(context.getEnvironment().getProperty("version")).as("version mismatch").isEqualTo(expectedVersion);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
