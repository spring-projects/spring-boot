/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link RestTemplateMetricsConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Jon Schneider
 * @author Raheela Aslam
 */
@ExtendWith(OutputCaptureExtension.class)
class RestTemplateMetricsConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class,
					HttpClientMetricsAutoConfiguration.class));

	@Test
	void restTemplateCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			RestTemplateBuilder builder = context.getBean(RestTemplateBuilder.class);
			validateRestTemplate(builder, registry);
		});
	}

	@Test
	void restTemplateWithRootUriIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			RestTemplateBuilder builder = context.getBean(RestTemplateBuilder.class);
			builder = builder.rootUri("/root");
			validateRestTemplate(builder, registry, "/root");
		});
	}

	@Test
	void restTemplateCanBeCustomizedManually() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MetricsRestTemplateCustomizer.class);
			RestTemplateBuilder customBuilder = new RestTemplateBuilder()
					.customizers(context.getBean(MetricsRestTemplateCustomizer.class));
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			validateRestTemplate(customBuilder, registry);
		});
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		this.contextRunner.withPropertyValues("management.metrics.web.client.max-uri-tags=2").run((context) -> {
			MeterRegistry registry = getInitializedMeterRegistry(context);
			assertThat(registry.get("http.client.requests").meters()).hasSize(2);
			assertThat(output).contains("Reached the maximum number of URI tags for 'http.client.requests'.")
					.contains("Are you using 'uriVariables'?");
		});
	}

	@Test
	void shouldNotDenyNorLogIfMaxUrisIsNotReached(CapturedOutput output) {
		this.contextRunner.withPropertyValues("management.metrics.web.client.max-uri-tags=5").run((context) -> {
			MeterRegistry registry = getInitializedMeterRegistry(context);
			assertThat(registry.get("http.client.requests").meters()).hasSize(3);
			assertThat(output).doesNotContain("Reached the maximum number of URI tags for 'http.client.requests'.")
					.doesNotContain("Are you using 'uriVariables'?");
		});
	}

	@Test
	void autoTimeRequestsCanBeConfigured() {
		this.contextRunner.withPropertyValues("management.metrics.web.client.request.autotime.enabled=true",
				"management.metrics.web.client.request.autotime.percentiles=0.5,0.7",
				"management.metrics.web.client.request.autotime.percentiles-histogram=true").run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					Timer timer = registry.get("http.client.requests").timer();
					HistogramSnapshot snapshot = timer.takeSnapshot();
					assertThat(snapshot.percentileValues()).hasSize(2);
					assertThat(snapshot.percentileValues()[0].percentile()).isEqualTo(0.5);
					assertThat(snapshot.percentileValues()[1].percentile()).isEqualTo(0.7);
				});
	}

	@Test
	void backsOffWhenRestTemplateBuilderIsMissing() {
		new ApplicationContextRunner().with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(HttpClientMetricsAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(DefaultRestTemplateExchangeTagsProvider.class)
						.doesNotHaveBean(MetricsRestTemplateCustomizer.class));
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableApplicationContext context) {
		MeterRegistry registry = context.getBean(MeterRegistry.class);
		RestTemplate restTemplate = context.getBean(RestTemplateBuilder.class).build();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		for (int i = 0; i < 3; i++) {
			server.expect(requestTo("/test/" + i)).andRespond(withStatus(HttpStatus.OK));
		}
		for (int i = 0; i < 3; i++) {
			restTemplate.getForObject("/test/" + i, String.class);
		}
		return registry;
	}

	private void validateRestTemplate(RestTemplateBuilder builder, MeterRegistry registry) {
		this.validateRestTemplate(builder, registry, "");
	}

	private void validateRestTemplate(RestTemplateBuilder builder, MeterRegistry registry, String rootUri) {
		RestTemplate restTemplate = mockRestTemplate(builder, rootUri);
		assertThat(registry.find("http.client.requests").meter()).isNull();
		assertThat(restTemplate.getForEntity("/projects/{project}", Void.class, "spring-boot").getStatusCode())
				.isEqualTo(HttpStatus.OK);
		assertThat(registry.get("http.client.requests").tags("uri", rootUri + "/projects/{project}").meter())
				.isNotNull();
	}

	private RestTemplate mockRestTemplate(RestTemplateBuilder builder, String rootUri) {
		RestTemplate restTemplate = builder.build();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		server.expect(requestTo(rootUri + "/projects/spring-boot")).andRespond(withStatus(HttpStatus.OK));
		return restTemplate;
	}

}
