/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to reproduce reported issues.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ReproTests {

	private ConfigurableApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void enableProfileViaApplicationProperties() {
		// gh-308
		SpringApplication application = new SpringApplication(Config.class);

		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run(
				"--spring.config.name=enableprofileviaapplicationproperties",
				"--spring.profiles.active=dev");
		assertThat(this.context.getEnvironment().acceptsProfiles("dev")).isTrue();
		assertThat(this.context.getEnvironment().acceptsProfiles("a")).isTrue();
	}

	@Test
	public void activeProfilesWithYamlAndCommandLine() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro";
		this.context = application.run(configName, "--spring.profiles.active=B");
		assertVersionProperty(this.context, "B", "B");
	}

	@Test
	public void activeProfilesWithYamlOnly() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro";
		this.context = application.run(configName);
		assertVersionProperty(this.context, "B", "B");
	}

	@Test
	public void orderActiveProfilesWithYamlOnly() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro-ordered";
		this.context = application.run(configName);
		assertVersionProperty(this.context, "B", "A", "B");
	}

	@Test
	public void commandLineBeatsProfilesWithYaml() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro";
		this.context = application.run(configName, "--spring.profiles.active=C");
		assertVersionProperty(this.context, "C", "C");
	}

	@Test
	public void orderProfilesWithYaml() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro";
		this.context = application.run(configName, "--spring.profiles.active=A,C");
		assertVersionProperty(this.context, "C", "A", "C");
	}

	@Test
	public void reverseOrderOfProfilesWithYaml() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro";
		this.context = application.run(configName, "--spring.profiles.active=C,A");
		assertVersionProperty(this.context, "A", "C", "A");
	}

	@Test
	public void activeProfilesWithYamlAndCommandLineAndNoOverride() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro-without-override";
		this.context = application.run(configName, "--spring.profiles.active=B");
		assertVersionProperty(this.context, "B", "B");
	}

	@Test
	public void activeProfilesWithYamlOnlyAndNoOverride() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro-without-override";
		this.context = application.run(configName);
		assertVersionProperty(this.context, null);
	}

	@Test
	public void commandLineBeatsProfilesWithYamlAndNoOverride() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro-without-override";
		this.context = application.run(configName, "--spring.profiles.active=C");
		assertVersionProperty(this.context, "C", "C");
	}

	@Test
	public void orderProfilesWithYamlAndNoOverride() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro-without-override";
		this.context = application.run(configName, "--spring.profiles.active=A,C");
		assertVersionProperty(this.context, "C", "A", "C");
	}

	@Test
	public void reverseOrderOfProfilesWithYamlAndNoOverride() {
		// gh-322, gh-342
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=activeprofilerepro-without-override";
		this.context = application.run(configName, "--spring.profiles.active=C,A");
		assertVersionProperty(this.context, "A", "C", "A");
	}

	private void assertVersionProperty(ConfigurableApplicationContext context,
			String expectedVersion, String... expectedActiveProfiles) {
		assertThat(context.getEnvironment().getActiveProfiles())
				.isEqualTo(expectedActiveProfiles);
		assertThat(context.getEnvironment().getProperty("version")).as("version mismatch")
				.isEqualTo(expectedVersion);
	}

	@Configuration
	public static class Config {

	}

}
