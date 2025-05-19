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

package org.springframework.boot.testcontainers;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.DatabaseConnectionDetails;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisabledIfDockerUnavailable
@ImportTestcontainers(LoadTimeWeaverAwareConsumerContainers.class)
class LoadTimeWeaverAwareConsumerImportTestcontainersTests implements LoadTimeWeaverAwareConsumerContainers {

	@Autowired
	private LoadTimeWeaverAwareConsumer consumer;

	@Test
	void loadTimeWeaverAwareBeanCanUseJdbcUrlFromContainerBasedConnectionDetails() {
		assertThat(this.consumer.jdbcUrl).isNotNull();
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		DataSource dataSource() {
			EmbeddedDatabaseFactory embeddedDatabaseFactory = new EmbeddedDatabaseFactory();
			embeddedDatabaseFactory.setGenerateUniqueDatabaseName(true);
			embeddedDatabaseFactory.setDatabaseType(EmbeddedDatabaseType.H2);
			return embeddedDatabaseFactory.getDatabase();
		}

		@Bean
		LoadTimeWeaverAwareConsumer loadTimeWeaverAwareConsumer(DatabaseConnectionDetails connectionDetails) {
			return new LoadTimeWeaverAwareConsumer(connectionDetails);
		}

	}

	static class LoadTimeWeaverAwareConsumer implements LoadTimeWeaverAware {

		private final String jdbcUrl;

		LoadTimeWeaverAwareConsumer(DatabaseConnectionDetails connectionDetails) {
			this.jdbcUrl = connectionDetails.getJdbcUrl();
		}

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		}

	}

}
