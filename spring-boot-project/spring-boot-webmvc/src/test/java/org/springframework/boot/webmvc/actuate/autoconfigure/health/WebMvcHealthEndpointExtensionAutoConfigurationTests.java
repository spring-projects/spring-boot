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

package org.springframework.boot.webmvc.actuate.autoconfigure.health;

import org.junit.jupiter.api.Test;

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
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.actuate.endpoint.web.AdditionalHealthEndpointPathsWebMvcHandlerMapping;
import org.springframework.boot.webmvc.autoconfigure.actuate.endpoint.web.WebMvcHealthEndpointExtensionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcHealthEndpointExtensionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class WebMvcHealthEndpointExtensionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				WebMvcHealthEndpointExtensionAutoConfiguration.class));

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withBean(DispatcherServlet.class)
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(HealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(AdditionalHealthEndpointPathsWebMvcHandlerMapping.class);
			});
	}

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void backsOffWithoutWebEndpointInfrastructure() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(EndpointAutoConfiguration.class))
			.withBean(DispatcherServlet.class)
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(AdditionalHealthEndpointPathsWebMvcHandlerMapping.class));
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

	}

}
