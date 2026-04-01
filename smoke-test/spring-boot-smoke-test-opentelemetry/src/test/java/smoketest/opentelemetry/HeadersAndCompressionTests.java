/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.opentelemetry;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsProperties;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingProperties;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.Transport;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HeadersAndCompressionTests {

	@Autowired
	private OtlpTracingProperties tracingProperties;

	@Autowired
	private OtlpLoggingProperties loggingProperties;

	@Autowired
	private OtlpMetricsProperties metricsProperties;

	@Test
	void generalHeadersAreMappedToAllSignalsWhenSignalSpecificNotSet() {
		assertThat(this.loggingProperties.getHeaders()).containsEntry("Authorization", "Bearer token123")
			.containsEntry("X-Custom-Header", "value1");
		assertThat(this.metricsProperties.getHeaders()).containsEntry("Authorization", "Bearer token123")
			.containsEntry("X-Custom-Header", "value1");
	}

	@Test
	void signalSpecificHeadersCompletelyOverrideGeneral() {
		assertThat(this.tracingProperties.getHeaders()).containsEntry("X-Trace-Header", "trace-value")
			.as("Signal-specific headers completely override general headers, not merge")
			.doesNotContainKey("Authorization");
	}

	@Test
	void compressionIsMapped() {
		assertThat(this.tracingProperties.getCompression()).isEqualTo(OtlpTracingProperties.Compression.GZIP);
		assertThat(this.loggingProperties.getCompression()).isEqualTo(OtlpLoggingProperties.Compression.GZIP);
		assertThat(this.metricsProperties.getCompressionMode())
			.isEqualTo(io.micrometer.registry.otlp.CompressionMode.GZIP);
	}

	@Test
	void timeoutIsMapped() {
		assertThat(this.tracingProperties.getTimeout()).isEqualTo(Duration.ofMillis(5000));
	}

	@Test
	void protocolIsMapped() {
		assertThat(this.tracingProperties.getTransport()).isEqualTo(Transport.HTTP);
		assertThat(this.loggingProperties.getTransport())
			.isEqualTo(org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport.HTTP);
	}

}
