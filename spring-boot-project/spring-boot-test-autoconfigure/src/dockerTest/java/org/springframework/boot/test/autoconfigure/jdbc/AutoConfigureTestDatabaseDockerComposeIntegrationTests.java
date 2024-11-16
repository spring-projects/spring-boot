/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabaseDockerComposeIntegrationTests.SetupDockerCompose;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase} with Docker Compose.
 *
 * @author Phillip Webb
 */
@SpringBootTest
@ContextConfiguration(initializers = SetupDockerCompose.class)
@AutoConfigureTestDatabase
@OverrideAutoConfiguration(enabled = false)
@DisabledIfDockerUnavailable
class AutoConfigureTestDatabaseDockerComposeIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void dataSourceIsNotReplaced() {
		assertThat(this.dataSource).isInstanceOf(HikariDataSource.class).isNotInstanceOf(EmbeddedDatabase.class);
	}

	@Configuration
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class Config {

	}

	static class SetupDockerCompose implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			try {
				Path composeFile = Files.createTempFile("", "-postgres-compose");
				String composeFileContent = new ClassPathResource("postgres-compose.yaml")
					.getContentAsString(StandardCharsets.UTF_8)
					.replace("{imageName}", TestImage.POSTGRESQL.toString());
				Files.writeString(composeFile, composeFileContent);
				TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
						"spring.docker.compose.skip.in-tests=false", "spring.docker.compose.stop.command=down",
						"spring.docker.compose.file=" + composeFile.toAbsolutePath().toString());
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

}
