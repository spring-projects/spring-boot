/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.docker.compose.otlp;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.Transport;
import org.springframework.boot.ssl.SslBundle;
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
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "otlp-ssl-compose.yaml", image = TestImage.GRAFANA_OTEL_LGTM,
			additionalResources = "ca.crt")
	void runWithSslCreatesConnectionDetails(OtlpTracingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("https://").endsWith("/v1/traces");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("https://").endsWith("/v1/traces");
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

}
