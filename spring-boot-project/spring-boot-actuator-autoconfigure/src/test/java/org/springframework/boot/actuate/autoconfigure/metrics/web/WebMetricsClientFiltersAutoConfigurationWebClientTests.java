/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.web;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.reactive.WebClientMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebMetricsClientFiltersAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
public class WebMetricsClientFiltersAutoConfigurationWebClientTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
					WebClientMetricsAutoConfiguration.class,
					SimpleMetricsExportAutoConfiguration.class,
					WebClientAutoConfiguration.class,
					WebMetricsClientFiltersAutoConfiguration.class));

	private ClientHttpConnector connector;

	@Rule
	public OutputCapture out = new OutputCapture();

	@Before
	public void setup() {
		this.connector = mock(ClientHttpConnector.class);
		given(this.connector.connect(any(), any(), any()))
				.willReturn(Mono.just(new MockClientHttpResponse(HttpStatus.OK)));
	}

	@Test
	public void afterMaxUrisReachedFurtherUrisAreDenied() {
		this.contextRunner
				.withPropertyValues("management.metrics.web.client.max-uri-tags=2")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.client.requests").meters()).hasSize(2);
					assertThat(this.out.toString())
							.contains("Reached the maximum number of URI tags "
									+ "for 'http.client.requests'. Are you using "
									+ "'uriVariables' on RestTemplate/WebClient calls?");
				});
	}

	@Test
	public void shouldNotDenyNorLogIfMaxUrisIsNotReached() {
		this.contextRunner
				.withPropertyValues("management.metrics.web.client.max-uri-tags=5")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.client.requests").meters()).hasSize(3);
					assertThat(this.out.toString())
							.doesNotContain("Reached the maximum number of URI tags "
									+ "for 'http.client.requests'. Are you using "
									+ "'uriVariables' on RestTemplate/WebClient calls?");
				});

	}

	private MeterRegistry getInitializedMeterRegistry(
			AssertableApplicationContext context) {
		WebClient.Builder builder = context.getBean(WebClient.Builder.class);
		WebClient webClient = builder.clientConnector(this.connector).build();
		MeterRegistry registry = context.getBean(MeterRegistry.class);
		for (int i = 0; i < 3; i++) {
			webClient.get().uri("http://example.org/projects/" + i).exchange().block();
		}
		return registry;
	}

}
