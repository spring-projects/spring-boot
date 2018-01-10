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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthEndpointAutoConfiguration} in a reactive environment.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class ReactiveHealthEndpointWebExtensionTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withUserConfiguration(HealthIndicatorAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class);

	@Test
	public void runShouldCreateExtensionBeans() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(ReactiveHealthEndpointWebExtension.class));
	}

	@Test
	public void runWhenHealthEndpointIsDisabledShouldNotCreateExtensionBeans() {
		this.contextRunner.withPropertyValues("management.endpoint.health.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ReactiveHealthEndpointWebExtension.class));
	}

	@Test
	public void runWithCustomHealthMappingShouldMapStatusCode() {
		this.contextRunner
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.run((context) -> {
					Object extension = context
							.getBean(ReactiveHealthEndpointWebExtension.class);
					HealthStatusHttpMapper mapper = (HealthStatusHttpMapper) ReflectionTestUtils
							.getField(extension, "statusHttpMapper");
					Map<String, Integer> statusMappings = mapper.getStatusMapping();
					assertThat(statusMappings).containsEntry("DOWN", 503);
					assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
					assertThat(statusMappings).containsEntry("CUSTOM", 500);
				});
	}

	@Test
	public void regularAndReactiveHealthIndicatorsMatch() {
		this.contextRunner.withUserConfiguration(HealthIndicatorsConfiguration.class)
				.run((context) -> {
					HealthEndpoint endpoint = context.getBean(HealthEndpoint.class);
					ReactiveHealthEndpointWebExtension extension = context
							.getBean(ReactiveHealthEndpointWebExtension.class);
					Health endpointHealth = endpoint.health();
					Health extensionHealth = extension.health(true).block().getBody();
					assertThat(endpointHealth.getDetails())
							.containsOnlyKeys("application", "first", "second");
					assertThat(extensionHealth.getDetails())
							.containsOnlyKeys("application", "first", "second");
				});
	}

	@Configuration
	static class HealthIndicatorsConfiguration {

		@Bean
		public HealthIndicator firstHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		public ReactiveHealthIndicator secondHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

	}

}
