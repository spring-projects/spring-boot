/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.redis;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisContainerConnectionDetailsFactory}.
 *
 * @author Yanming Zhou
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class RedisContainerConnectionDetailsFactoryIntegrationTests {

	@Autowired
	private RedisConnectionDetails connectionDetails;

	@Test
	void connectionDetailsShouldBeCreatedByRedisContainerConnectionDetailsFactory() {
		assertThat(this.connectionDetails.getClass().getEnclosingClass())
			.isSameAs(RedisContainerConnectionDetailsFactory.class);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(ServiceConnectionAutoConfiguration.class)
	static class ContainerConfig {

		@Bean
		@ServiceConnection
		RedisContainer redis() {
			return new RedisContainer();
		}

	}

}
