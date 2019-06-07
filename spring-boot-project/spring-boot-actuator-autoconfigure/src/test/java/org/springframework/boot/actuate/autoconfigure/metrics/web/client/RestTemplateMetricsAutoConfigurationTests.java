/*
 * Copyright 2012-2019 the original author or authors.
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
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link RestTemplateMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Jon Schneider
 */
public class RestTemplateMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class));

	@Rule
	public OutputCapture out = new OutputCapture();

	@Test
	public void restTemplateCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			RestTemplate restTemplate = context.getBean(RestTemplateBuilder.class).build();
			validateRestTemplate(restTemplate, registry);
		});
	}

	@Test
	public void restTemplateCanBeCustomizedManually() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MetricsRestTemplateCustomizer.class);
			RestTemplate restTemplate = new RestTemplateBuilder()
					.customizers(context.getBean(MetricsRestTemplateCustomizer.class)).build();
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			validateRestTemplate(restTemplate, registry);
		});
	}

	@Test
	public void afterMaxUrisReachedFurtherUrisAreDenied() {
		this.contextRunner.withPropertyValues("management.metrics.web.client.max-uri-tags=2").run((context) -> {
			MeterRegistry registry = getInitializedMeterRegistry(context);
			assertThat(registry.get("http.client.requests").meters()).hasSize(2);
			assertThat(this.out.toString())
					.contains("Reached the maximum number of URI tags for 'http.client.requests'.");
			assertThat(this.out.toString()).contains("Are you using 'uriVariables' on RestTemplate calls?");
		});
	}

	@Test
	public void shouldNotDenyNorLogIfMaxUrisIsNotReached() {
		this.contextRunner.withPropertyValues("management.metrics.web.client.max-uri-tags=5").run((context) -> {
			MeterRegistry registry = getInitializedMeterRegistry(context);
			assertThat(registry.get("http.client.requests").meters()).hasSize(3);
			assertThat(this.out.toString())
					.doesNotContain("Reached the maximum number of URI tags for 'http.client.requests'.");
			assertThat(this.out.toString()).doesNotContain("Are you using 'uriVariables' on RestTemplate calls?");
		});
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

	private void validateRestTemplate(RestTemplate restTemplate, MeterRegistry registry) {
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		server.expect(requestTo("/test")).andRespond(withStatus(HttpStatus.OK));
		assertThat(registry.find("http.client.requests").meter()).isNull();
		assertThat(restTemplate.getForEntity("/test", Void.class).getStatusCode()).isEqualTo(HttpStatus.OK);
		registry.get("http.client.requests").meter();
	}

}
