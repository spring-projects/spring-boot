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

package org.springframework.boot.testcontainers.service.connection;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory.ContainerConnectionDetails;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionAutoConfigurationRegistrar} to verify that
 * annotations on {@link Bean @Bean} methods are available in
 * {@link ContainerConnectionSource}.
 *
 * @author Daeho Kwon
 */
class ServiceConnectionAutoConfigurationRegistrarTests {

	@Test
	void sslAnnotationOnBeanMethodShouldBeDetectedInContainerConnectionSource() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(WithServiceConnectionAutoConfiguration.class, ContainerConfiguration.class);
			context.refresh();
			ContainerConnectionDetails<?> details = (ContainerConnectionDetails<?>) context
				.getBean(DatabaseConnectionDetails.class);
			assertThat(details.hasAnnotation(Ssl.class)).isTrue();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(ServiceConnectionAutoConfiguration.class)
	static class WithServiceConnectionAutoConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		@ServiceConnection
		@Ssl
		PostgreSQLContainer container() {
			return mock(PostgreSQLContainer.class);
		}

	}

}
