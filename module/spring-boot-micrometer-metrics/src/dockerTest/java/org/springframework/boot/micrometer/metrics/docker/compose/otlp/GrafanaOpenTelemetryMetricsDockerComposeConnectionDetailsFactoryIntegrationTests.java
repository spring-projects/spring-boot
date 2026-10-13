/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.docker.compose.otlp;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenTelemetryMetricsDockerComposeConnectionDetailsFactory}
 * using {@link TestImage#GRAFANA_OTEL_LGTM}.
 *
 * @author Eddú Meléndez
 */
class GrafanaOpenTelemetryMetricsDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "otlp-compose.yaml", image = TestImage.GRAFANA_OTEL_LGTM)
	void runCreatesConnectionDetails(OtlpMetricsConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl()).startsWith("http://").endsWith("/v1/metrics");
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "otlp-ssl-compose.yaml", image = TestImage.GRAFANA_OTEL_LGTM,
			additionalResources = "ca.crt")
	void runWithSslCreatesConnectionDetails(OtlpMetricsConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl()).startsWith("https://").endsWith("/v1/metrics");
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

}
