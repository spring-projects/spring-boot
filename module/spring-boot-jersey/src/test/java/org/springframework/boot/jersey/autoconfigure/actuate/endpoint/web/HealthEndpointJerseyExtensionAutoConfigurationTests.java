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

package org.springframework.boot.jersey.autoconfigure.actuate.endpoint.web;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.WithTestEndpointOutcomeExposureContributor;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.jersey.autoconfigure.actuate.endpoint.web.HealthEndpointJerseyExtensionAutoConfiguration.JerseyAdditionalHealthEndpointPathsResourcesRegistrar;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthEndpointJerseyExtensionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class HealthEndpointJerseyExtensionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				HealthEndpointJerseyExtensionAutoConfiguration.class));

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalJerseyHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(HealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(JerseyAdditionalHealthEndpointPathsResourcesRegistrar.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class HealthIndicatorsConfiguration {

		@Bean
		HealthIndicator simpleHealthIndicator() {
			return () -> Health.up().withDetail("counter", 42).build();
		}

		@Bean
		HealthIndicator additionalHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		ReactiveHealthIndicator reactiveHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

	}

}
