/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.client;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link RestTemplateObservationConfiguration}.
 *
 * @author Brian Clozel
 */
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("removal")
class RestTemplateObservationConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(ObservationRegistry.class, TestObservationRegistry::create)
			.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
					RestTemplateAutoConfiguration.class, HttpClientObservationsAutoConfiguration.class));

	@Test
	void contributesCustomizerBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ObservationRestTemplateCustomizer.class)
				.doesNotHaveBean(DefaultRestTemplateExchangeTagsProvider.class));
	}

	@Test
	void restTemplateCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			RestTemplate restTemplate = buildRestTemplate(context);
			restTemplate.getForEntity("/projects/{project}", Void.class, "spring-boot");
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			TestObservationRegistryAssert.assertThat(registry)
					.hasObservationWithNameEqualToIgnoringCase("http.client.requests");
		});
	}

	@Test
	void restTemplateCreatedWithBuilderUsesCustomConventionName() {
		final String observationName = "test.metric.name";
		this.contextRunner.withPropertyValues("management.observations.http.client.requests.name=" + observationName)
				.run((context) -> {
					RestTemplate restTemplate = buildRestTemplate(context);
					restTemplate.getForEntity("/projects/{project}", Void.class, "spring-boot");
					TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
					TestObservationRegistryAssert.assertThat(registry)
							.hasObservationWithNameEqualToIgnoringCase(observationName);
				});
	}

	@Test
	void restTemplateCreatedWithBuilderUsesCustomMetricName() {
		final String metricName = "test.metric.name";
		this.contextRunner.withPropertyValues("management.metrics.web.client.request.metric-name=" + metricName)
				.run((context) -> {
					RestTemplate restTemplate = buildRestTemplate(context);
					restTemplate.getForEntity("/projects/{project}", Void.class, "spring-boot");
					TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
					TestObservationRegistryAssert.assertThat(registry)
							.hasObservationWithNameEqualToIgnoringCase(metricName);
				});
	}

	@Test
	void restTemplateCreatedWithBuilderUsesCustomTagsProvider() {
		this.contextRunner.withUserConfiguration(CustomTagsConfiguration.class).run((context) -> {
			RestTemplate restTemplate = buildRestTemplate(context);
			restTemplate.getForEntity("/projects/{project}", Void.class, "spring-boot");
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("http.client.requests")
					.that().hasLowCardinalityKeyValue("project", "spring-boot");
		});
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		this.contextRunner.with(MetricsRun.simple()).withPropertyValues("management.metrics.web.client.max-uri-tags=2")
				.run((context) -> {

					RestTemplate restTemplate = context.getBean(RestTemplateBuilder.class).build();
					MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
					for (int i = 0; i < 3; i++) {
						server.expect(requestTo("/test/" + i)).andRespond(withStatus(HttpStatus.OK));
					}
					for (int i = 0; i < 3; i++) {
						restTemplate.getForObject("/test/" + i, String.class);
					}
					TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
					TestObservationRegistryAssert.assertThat(registry);
					// TODO: check observation count for name
					assertThat(output).contains("Reached the maximum number of URI tags for 'http.client.requests'.")
							.contains("Are you using 'uriVariables'?");
				});
	}

	@Test
	void backsOffWhenRestTemplateBuilderIsMissing() {
		new ApplicationContextRunner().with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
						HttpClientObservationsAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(DefaultRestTemplateExchangeTagsProvider.class)
						.doesNotHaveBean(ObservationRestTemplateCustomizer.class));
	}

	private RestTemplate buildRestTemplate(AssertableApplicationContext context) {
		RestTemplate restTemplate = context.getBean(RestTemplateBuilder.class).build();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		server.expect(requestTo("/projects/spring-boot")).andRespond(withStatus(HttpStatus.OK));
		return restTemplate;
	}

	@Configuration
	static class CustomTagsConfiguration {

		@Bean
		CustomTagsProvider customTagsProvider() {
			return new CustomTagsProvider();
		}

	}

	@Deprecated(since = "3.0.0", forRemoval = true)
	static class CustomTagsProvider implements RestTemplateExchangeTagsProvider {

		@Override
		public Iterable<Tag> getTags(String urlTemplate, HttpRequest request, ClientHttpResponse response) {
			return Tags.of("project", "spring-boot");
		}

	}

}
