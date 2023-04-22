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

package org.springframework.boot.testcontainers.service.connection.zipkin;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConnectionDetails;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipkinContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class ZipkinContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final GenericContainer<?> container = new GenericContainer<>("openzipkin/zipkin:2.23.2")
		.withExposedPorts(9411);

	@Autowired
	private ZipkinConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToRabbitContainer() {
		assertThat(this.connectionDetails.getSpanEndpoint())
			.startsWith("http://" + container.getHost() + ":" + container.getMappedPort(9411));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(ZipkinAutoConfiguration.class)
	static class TestConfiguration {

	}

}
