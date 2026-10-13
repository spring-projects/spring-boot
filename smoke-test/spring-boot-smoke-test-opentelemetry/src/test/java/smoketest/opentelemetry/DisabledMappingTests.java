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
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "management.opentelemetry.map-environment-variables=false")
class DisabledMappingTests {

	@Autowired
	private OtlpTracingProperties tracingProperties;

	@Autowired
	private OtlpLoggingProperties loggingProperties;

	@Autowired
	private OtlpMetricsProperties metricsProperties;

	@Test
	void environmentVariablesAreNotMappedWhenDisabled() {
		assertThat(this.tracingProperties.getEndpoint()).isNull();
		assertThat(this.loggingProperties.getEndpoint()).isNull();
		assertThat(this.metricsProperties.getUrl()).isNull();
	}

}
