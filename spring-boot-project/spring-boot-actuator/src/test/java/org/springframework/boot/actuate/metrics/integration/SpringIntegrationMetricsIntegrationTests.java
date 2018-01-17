/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.integration;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringIntegrationMetrics}.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
public class SpringIntegrationMetricsIntegrationTests {

	@Autowired
	TestSpringIntegrationApplication.TempConverter converter;

	@Autowired
	MeterRegistry registry;

	@Test
	public void springIntegrationMetrics() {
		this.converter.fahrenheitToCelsius(68.0);
		assertThat(this.registry.find("spring.integration.channel.sends")
				.tags("channel", "convert.input").value(Statistic.Count, 1).meter())
						.isPresent();
		assertThat(this.registry.find("spring.integration.handler.duration.min").meter())
				.isPresent();
		assertThat(this.registry.find("spring.integration.sourceNames").meter())
				.isPresent();
	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	public static class TestSpringIntegrationApplication {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		public IntegrationManagementConfigurer integrationManagementConfigurer() {
			IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
			configurer.setDefaultCountsEnabled(true);
			configurer.setDefaultStatsEnabled(true);
			return configurer;
		}

		@Bean
		public SpringIntegrationMetrics springIntegrationMetrics(
				IntegrationManagementConfigurer configurer, MeterRegistry registry) {
			SpringIntegrationMetrics springIntegrationMetrics = new SpringIntegrationMetrics(
					configurer);
			springIntegrationMetrics.bindTo(registry);
			return springIntegrationMetrics;
		}

		@Bean
		public IntegrationFlow convert() {
			return (f) -> f
					.transform((payload) -> "{\"fahrenheit\":" + payload + "}",
							(e) -> e.id("toJson"))
					.handle(String.class, this::fahrenheitToCelsius,
							(e) -> e.id("temperatureConverter"))
					.transform(this::extractResult, e -> e.id("toResponse"));
		}

		private double extractResult(String json) {
			try {
				return (double) new ObjectMapper().readValue(json, Map.class)
						.get("celsius");
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		private String fahrenheitToCelsius(String payload, Map<String, Object> headers) {
			try {
				double fahrenheit = (double) new ObjectMapper()
						.readValue(payload, Map.class).get("fahrenheit");
				double celsius = (fahrenheit - 32) * (5.0 / 9.0);
				return "{\"celsius\":" + celsius + "}";
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		@MessagingGateway
		public interface TempConverter {

			@Gateway(requestChannel = "convert.input")
			double fahrenheitToCelsius(double fahrenheit);

		}

	}

}
