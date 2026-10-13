/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.opentelemetry;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsProperties;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SignalSpecificPrecedenceTests {

	@Autowired
	private OtlpTracingProperties tracingProperties;

	@Autowired
	private OtlpLoggingProperties loggingProperties;

	@Autowired
	private OtlpMetricsProperties metricsProperties;

	@Test
	void signalSpecificEndpointTakesPrecedenceOverGeneral() {
		assertThat(this.tracingProperties.getEndpoint()).isEqualTo("http://localhost:4319/custom/traces");
		assertThat(this.loggingProperties.getEndpoint()).isEqualTo("http://localhost:4321/custom/logs");
		assertThat(this.metricsProperties.getUrl()).isEqualTo("http://localhost:4320/custom/metrics");
	}

}
