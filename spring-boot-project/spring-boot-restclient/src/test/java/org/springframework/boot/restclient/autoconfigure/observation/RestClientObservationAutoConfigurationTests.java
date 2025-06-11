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

package org.springframework.boot.restclient.autoconfigure.observation;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientObservationAutoConfiguration;
import org.springframework.boot.restclient.observation.ObservationRestClientCustomizer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link RestClientObservationAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class RestClientObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(ObservationRegistry.class, TestObservationRegistry::create)
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class, RestClientAutoConfiguration.class,
				RestClientObservationAutoConfiguration.class));

	@Test
	void contributesCustomizerBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ObservationRestClientCustomizer.class));
	}

	@Test
	void restClientCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			RestClient restClient = buildRestClient(context);
			restClient.get().uri("/projects/{project}", "spring-boot").retrieve().toBodilessEntity();
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			assertThat(registry).hasObservationWithNameEqualToIgnoringCase("http.client.requests");
		});
	}

	@Test
	void restClientCreatedWithBuilderUsesCustomConventionName() {
		final String observationName = "test.metric.name";
		this.contextRunner.withPropertyValues("management.observations.http.client.requests.name=" + observationName)
			.run((context) -> {
				RestClient restClient = buildRestClient(context);
				restClient.get().uri("/projects/{project}", "spring-boot").retrieve().toBodilessEntity();
				TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
				assertThat(registry).hasObservationWithNameEqualToIgnoringCase(observationName);
			});
	}

	@Test
	void restClientCreatedWithBuilderUsesCustomConvention() {
		this.contextRunner.withUserConfiguration(CustomConvention.class).run((context) -> {
			RestClient restClient = buildRestClient(context);
			restClient.get().uri("/projects/{project}", "spring-boot").retrieve().toBodilessEntity();
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			assertThat(registry).hasObservationWithNameEqualTo("http.client.requests")
				.that()
				.hasLowCardinalityKeyValue("project", "spring-boot");
		});
	}

	@Test
	void backsOffWhenRestClientBuilderIsMissing() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
					RestClientObservationAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ObservationRestClientCustomizer.class));
	}

	private RestClient buildRestClient(AssertableApplicationContext context) {
		RestClientWithMockServer restClientWithMockServer = buildRestClientAndMockServer(context);
		restClientWithMockServer.mockServer()
			.expect(requestTo("/projects/spring-boot"))
			.andRespond(withStatus(HttpStatus.OK));
		return restClientWithMockServer.restClient();
	}

	private RestClientWithMockServer buildRestClientAndMockServer(AssertableApplicationContext context) {
		Builder builder = context.getBean(Builder.class);
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("/projects/spring-boot")).andRespond(withStatus(HttpStatus.OK));
		return new RestClientWithMockServer(builder.build(), server);
	}

	private record RestClientWithMockServer(RestClient restClient, MockRestServiceServer mockServer) {
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConventionConfiguration {

		@Bean
		CustomConvention customConvention() {
			return new CustomConvention();
		}

	}

	static class CustomConvention extends DefaultClientRequestObservationConvention {

		@Override
		public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
			return super.getLowCardinalityKeyValues(context).and("project", "spring-boot");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MetricsConfiguration {

		@Bean
		MeterObservationHandler<Context> meterObservationHandler(MeterRegistry registry) {
			return new DefaultMeterObservationHandler(registry);
		}

	}

}
