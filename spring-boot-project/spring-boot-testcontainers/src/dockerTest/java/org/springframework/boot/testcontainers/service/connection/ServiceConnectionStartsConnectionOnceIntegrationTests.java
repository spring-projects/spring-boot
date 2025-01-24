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

package org.springframework.boot.testcontainers.service.connection;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to ensure containers are started only once.
 *
 * @author Phillip Webb
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class ServiceConnectionStartsConnectionOnceIntegrationTests {

	@Container
	@ServiceConnection
	static final StartCountingPostgreSQLContainer postgres = TestImage
		.container(StartCountingPostgreSQLContainer.class);

	@Test
	void startedOnlyOnce() {
		assertThat(postgres.startCount.get()).isOne();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

	}

	static class StartCountingPostgreSQLContainer extends PostgreSQLContainer<StartCountingPostgreSQLContainer> {

		final AtomicInteger startCount = new AtomicInteger();

		StartCountingPostgreSQLContainer(DockerImageName dockerImageName) {
			super(dockerImageName);
		}

		@Override
		public void start() {
			this.startCount.incrementAndGet();
			super.start();
		}

	}

}
