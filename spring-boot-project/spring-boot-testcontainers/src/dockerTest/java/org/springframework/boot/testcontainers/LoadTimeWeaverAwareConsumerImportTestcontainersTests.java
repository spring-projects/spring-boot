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

package org.springframework.boot.testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

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
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		LoadTimeWeaverAwareConsumer loadTimeWeaverAwareConsumer(JdbcConnectionDetails connectionDetails) {
			return new LoadTimeWeaverAwareConsumer(connectionDetails);
		}

	}

	static class LoadTimeWeaverAwareConsumer implements LoadTimeWeaverAware {

		private final String jdbcUrl;

		LoadTimeWeaverAwareConsumer(JdbcConnectionDetails connectionDetails) {
			this.jdbcUrl = connectionDetails.getJdbcUrl();
		}

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		}

	}

}
