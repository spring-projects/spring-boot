/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory.ContainerConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionAutoConfigurationRegistrar}.
 *
 * @author Daeho Kwon
 */
class ServiceConnectionAutoConfigurationRegistrarTests {

	@Test
	void sslAnnotationOnBeanMethodShouldBeDetectedInContainerConnectionSource() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class))
			.withUserConfiguration(ContainerConfiguration.class)
			.run((context) -> {
				ContainerConnectionDetails<?> connectionDetails = (ContainerConnectionDetails<?>) context
					.getBean(DatabaseConnectionDetails.class);
				assertThat(connectionDetails.hasAnnotation(Ssl.class)).isTrue();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		@ServiceConnection
		@Ssl
		PostgreSQLContainer container() {
			return mock();
		}

	}

}
