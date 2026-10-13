/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtelCollectorOltpLoggingContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class OtelCollectorOltpLoggingContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final GenericContainer<?> container = TestImage.OTEL_COLLECTOR
		.container((setup) -> setup.withExposedPorts(4317, 4318));

	@Autowired
	private OtlpLoggingConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToOpenTelemetryContainer() {
		assertThat(this.connectionDetails.getUrl(Transport.HTTP))
			.isEqualTo("http://" + container.getHost() + ":" + container.getMappedPort(4318) + "/v1/logs");
		assertThat(this.connectionDetails.getUrl(Transport.GRPC))
			.isEqualTo("http://" + container.getHost() + ":" + container.getMappedPort(4317) + "/v1/logs");
		assertThat(this.connectionDetails.getSslBundle()).isNull();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(OtlpLoggingAutoConfiguration.class)
	static class TestConfiguration {

	}

}
