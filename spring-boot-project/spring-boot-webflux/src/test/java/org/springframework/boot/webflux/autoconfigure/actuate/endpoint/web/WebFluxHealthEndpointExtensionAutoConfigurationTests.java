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

package org.springframework.boot.webflux.autoconfigure.actuate.endpoint.web;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.WithTestEndpointOutcomeExposureContributor;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.actuate.endpoint.web.AdditionalHealthEndpointPathsWebFluxHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxHealthEndpointExtensionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class WebFluxHealthEndpointExtensionAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				WebFluxHealthEndpointExtensionAutoConfiguration.class));

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalReactiveHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(ReactiveHealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(AdditionalHealthEndpointPathsWebFluxHandlerMapping.class);
			});
	}

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void backsOffWithoutWebEndpointInfrastructure() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(EndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(AdditionalHealthEndpointPathsWebFluxHandlerMapping.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class HealthIndicatorsConfiguration {

		@Bean
		ReactiveHealthIndicator simpleHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

		@Bean
		HealthIndicator additionalHealthIndicator() {
			return () -> Health.up().build();
		}

	}

}
