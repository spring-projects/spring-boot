/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtlpLoggingDockerComposeConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
class OtelCollectorOtlpLoggingDockerComposeConnectionDetailsFactoryTests {

	@DockerComposeTest(composeFile = "otlp-compose.yaml", image = TestImage.OTEL_COLLECTOR)
	void runCreatesConnectionDetails(OtlpLoggingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("http://").endsWith("/v1/logs");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("http://").endsWith("/v1/logs");
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "otlp-ssl-compose.yaml", image = TestImage.OTEL_COLLECTOR,
			additionalResources = "ca.crt")
	void runWithSslCreatesConnectionDetails(OtlpLoggingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("https://").endsWith("/v1/logs");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("https://").endsWith("/v1/logs");
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

}
