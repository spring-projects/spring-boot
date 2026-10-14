/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests for {@link OtlpLoggingDockerComposeConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 * @author WooSeung Kim
 */
class OtelCollectorOtlpLoggingDockerComposeConnectionDetailsFactoryTests {

	@DockerComposeTest(composeFile = "otlp-compose.yaml", image = TestImage.OTEL_COLLECTOR)
	void runCreatesConnectionDetails(OtlpLoggingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("http://").endsWith("/v1/logs");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("http://").endsWith("/v1/logs");
	}

	@DockerComposeTest(composeFile = "otlp-http-only-compose.yaml", image = TestImage.OTEL_COLLECTOR)
	void runWithOnlyHttpPortCreatesConnectionDetails(OtlpLoggingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("http://").endsWith("/v1/logs");
		assertThatIllegalStateException().isThrownBy(() -> connectionDetails.getUrl(Transport.GRPC))
			.withMessage("No host port mapping found for container port 4317");
	}

}
