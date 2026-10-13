/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.grafana.LgtmStackContainer;
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
 * Tests for {@link GrafanaOtlpLoggingContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class GrafanaOtlpLoggingContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final LgtmStackContainer container = TestImage.forContainer(LgtmStackContainer.class);

	@Autowired
	private OtlpLoggingConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToOpenTelemetryContainer() {
		assertThat(this.connectionDetails.getUrl(Transport.GRPC))
			.isEqualTo("%s/v1/logs".formatted(container.getOtlpGrpcUrl()));
		assertThat(this.connectionDetails.getUrl(Transport.HTTP))
			.isEqualTo("%s/v1/logs".formatted(container.getOtlpHttpUrl()));
		assertThat(this.connectionDetails.getSslBundle()).isNull();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(OtlpLoggingAutoConfiguration.class)
	static class TestConfiguration {

	}

}
