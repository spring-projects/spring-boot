/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.docker.compose.otlp;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.Transport;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenTelemetryTracingDockerComposeConnectionDetailsFactory}
 * using {@link TestImage#GRAFANA_OTEL_LGTM}.
 *
 * @author Eddú Meléndez
 */
class GrafanaOpenTelemetryTracingDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "otlp-compose.yaml", image = TestImage.GRAFANA_OTEL_LGTM)
	void runCreatesConnectionDetails(OtlpTracingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("http://").endsWith("/v1/traces");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("http://").endsWith("/v1/traces");
	}

}
