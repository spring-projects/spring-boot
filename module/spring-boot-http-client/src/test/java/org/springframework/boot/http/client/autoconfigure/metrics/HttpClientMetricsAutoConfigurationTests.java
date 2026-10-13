/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.metrics;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpClientMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class HttpClientMetricsAutoConfigurationTests {

	@Test
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(HttpClientMetricsAutoConfiguration.class, MetricsAutoConfiguration.class))
			.withBean(SimpleMeterRegistry.class)
			.withPropertyValues("management.metrics.web.client.max-uri-tags=2")
			.run((context) -> {
				MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
				for (int i = 0; i < 3; i++) {
					meterRegistry.timer("http.client.requests", "uri", "/test/" + i).record(Duration.ofSeconds(1));
				}
				assertThat(meterRegistry.find("http.client.requests").timers()).hasSize(2);
				assertThat(output).contains("Reached the maximum number of 'uri' tags for 'http.client.requests'.")
					.contains("Are you using 'uriVariables'?");
			});
	}

}
