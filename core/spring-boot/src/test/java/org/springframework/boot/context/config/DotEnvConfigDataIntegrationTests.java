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

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for importing {@code .env} files using {@code spring.config.import}.
 *
 * @author Sharang Gupta
 */
class DotEnvConfigDataIntegrationTests {

	private SpringApplication application;

	@TempDir
	File temp;

	@BeforeEach
	void setup() {
		this.application = new SpringApplication(Config.class);
		this.application.setWebApplicationType(WebApplicationType.NONE);
	}

	@Test
	@WithResource(name = ".env", content = """
			# local development settings
			export SPRING_APPLICATION_NAME=dotenv-app
			MY_SOME_KEY="a quoted value"
			PLAIN=value
			""")
	void runWhenImportsDotEnvFromClasspathThenPropertiesAreAvailableUsingEnvironmentVariableNames() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.import=optional:classpath:.env");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("PLAIN")).isEqualTo("value");
		assertThat(environment.getProperty("spring.application.name")).isEqualTo("dotenv-app");
		assertThat(environment.getProperty("my.some-key")).isEqualTo("a quoted value");
		assertThat(Binder.get(environment).bind("my.some-key", String.class).get()).isEqualTo("a quoted value");
	}

	@Test
	void runWhenImportsDotEnvFromFileSystemThenPropertiesAreAvailable() throws Exception {
		File file = new File(this.temp, ".env");
		Files.writeString(file.toPath(), "DATABASE_PASSWORD=s3cr3t\nDATABASE_URL=jdbc:h2:mem:test\n");
		ConfigurableApplicationContext context = this.application
			.run("--spring.config.import=optional:file:" + file.getAbsolutePath());
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("database.password")).isEqualTo("s3cr3t");
		assertThat(environment.getProperty("database.url")).isEqualTo("jdbc:h2:mem:test");
	}

	@Test
	void runWhenOptionalDotEnvFileDoesNotExistThenStartsNormally() {
		File file = new File(this.temp, ".env");
		assertThat(file).doesNotExist();
		ConfigurableApplicationContext context = this.application
			.run("--spring.config.import=optional:file:" + file.getAbsolutePath());
		assertThat(context.getEnvironment().getProperty("DATABASE_URL")).isNull();
	}

	@Test
	void runWhenRequiredDotEnvFileDoesNotExistThenFails() {
		File file = new File(this.temp, ".env");
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
			.isThrownBy(() -> this.application.run("--spring.config.import=file:" + file.getAbsolutePath()))
			.withMessageContaining(".env");
	}

	@Test
	@WithResource(name = "application.properties", content = """
			greeting=Hello ${USER_NAME}
			my.override=from-application-properties
			""")
	@WithResource(name = ".env", content = """
			USER_NAME=Boot
			MY_OVERRIDE=from-dotenv
			""")
	void runWhenDotEnvIsImportedThenItsValuesCanBeUsedInPlaceholdersAndTakePrecedenceOverImportingFile() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.import=optional:classpath:.env");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("greeting")).isEqualTo("Hello Boot");
		assertThat(environment.getProperty("my.override")).isEqualTo("from-dotenv");
	}

	@Test
	@WithResource(name = ".env", content = """
			FIRST=1
			SECOND=two
			""")
	void runWhenDotEnvIsImportedThenOriginsPointToTheFileLocation() {
		ConfigurableApplicationContext context = this.application.run("--spring.config.import=optional:classpath:.env");
		ConfigurableEnvironment environment = context.getEnvironment();
		PropertySource<?> source = environment.getPropertySources()
			.stream()
			.filter((candidate) -> candidate.getName().contains(".env"))
			.findFirst()
			.orElseThrow();
		Origin origin = OriginLookup.getOrigin(source, "SECOND");
		assertThat(origin).isInstanceOfSatisfying(TextResourceOrigin.class, (textOrigin) -> {
			assertThat(String.valueOf(textOrigin.getResource())).contains(".env");
			assertThat(textOrigin.getLocation()).hasToString("2:8");
		});
	}

	@Test
	@WithResource(name = "application.env", content = "FROM_APPLICATION_ENV=found\n")
	@WithResource(name = "application-dev.env", content = "FROM_PROFILE_ENV=profile\n")
	void runWhenApplicationDotEnvIsOnTheClasspathThenItIsLoadedLikeOtherApplicationConfigFiles() {
		ConfigurableApplicationContext context = this.application.run("--spring.profiles.active=dev");
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("from.application.env")).isEqualTo("found");
		assertThat(environment.getProperty("from.profile.env")).isEqualTo("profile");
	}

	@Test
	@WithResource(name = "application.properties", content = "from.properties=properties\nsame.key=properties\n")
	@WithResource(name = "application.yaml",
			content = "from.yaml: yaml\nsame.key: yaml\nonly.yaml-and.properties: yaml\n")
	@WithResource(name = "application.env", content = "FROM_ENV=env\nSAME_KEY=env\n")
	void runWhenApplicationFilesInSameLocationThenPrecedenceIsEnvThenPropertiesThenYaml() {
		ConfigurableApplicationContext context = this.application.run();
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getProperty("from.env")).isEqualTo("env");
		assertThat(environment.getProperty("from.properties")).isEqualTo("properties");
		assertThat(environment.getProperty("from.yaml")).isEqualTo("yaml");
		assertThat(environment.getProperty("same.key")).isEqualTo("env");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
