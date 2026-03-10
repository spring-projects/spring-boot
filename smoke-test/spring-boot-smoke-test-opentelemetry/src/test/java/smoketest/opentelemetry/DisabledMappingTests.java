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
