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

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersImportWithPropertiesInjectedIntoLoadTimeWeaverAwareBeanIntegrationTests.Containers;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
@DisabledIfDockerUnavailable
@ImportTestcontainers(Containers.class)
class TestcontainersImportWithPropertiesInjectedIntoLoadTimeWeaverAwareBeanIntegrationTests {

	// gh-38913

	@Test
	void starts() {
	}

	@TestConfiguration
	@EnableConfigurationProperties(MockDataSourceProperties.class)
	static class Config {

		@Bean
		MockEntityManager mockEntityManager(MockDataSourceProperties properties) {
			return new MockEntityManager();
		}

	}

	static class MockEntityManager implements LoadTimeWeaverAware {

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		}

	}

	@ConfigurationProperties("spring.datasource")
	public static class MockDataSourceProperties {

		private String url;

		public String getUrl() {
			return this.url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

	}

	static class Containers {

		@Container
		static PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageNames.postgresql());

		@DynamicPropertySource
		static void setConnectionProperties(DynamicPropertyRegistry registry) {
			registry.add("spring.datasource.url", container::getJdbcUrl);
			registry.add("spring.datasource.password", container::getPassword);
			registry.add("spring.datasource.username", container::getUsername);
		}

	}

}
