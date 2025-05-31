/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointReactiveWebExtensionConfiguration.WebFluxAdditionalHealthEndpointPathsConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointWebExtensionConfiguration.JerseyAdditionalHealthEndpointPathsConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointWebExtensionConfiguration.MvcAdditionalHealthEndpointPathsConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthEndpointAutoConfiguration} when running in a Cloud Foundry
 * environment.
 *
 * This class isolates Cloud Foundry-specific behavior to avoid coupling such concerns
 * with general auto-configuration tests.
 *
 * @author Daeho Kwon
 */
class CloudFoundryHealthEndpointConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(
				AutoConfigurations.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class));

	private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(
				AutoConfigurations.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
						WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class));

	@Test
	void additionalHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.cloudfoundry.exposure.include=*", "spring.main.cloud-platform=cloud_foundry")
			.run((context) -> {
				assertThat(context).hasSingleBean(MvcAdditionalHealthEndpointPathsConfiguration.class);
				assertThat(context).hasNotFailed();
			});
	}

	@Test
	void additionalJerseyHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(DispatcherServlet.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.cloudfoundry.exposure.include=*", "spring.main.cloud-platform=cloud_foundry")
			.run((context) -> {
				assertThat(context).hasSingleBean(JerseyAdditionalHealthEndpointPathsConfiguration.class);
				assertThat(context).hasNotFailed();
			});
	}

	@Test
	void additionalReactiveHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.reactiveContextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.cloudfoundry.exposure.include=*", "spring.main.cloud-platform=cloud_foundry")
			.run((context) -> {
				assertThat(context).hasSingleBean(WebFluxAdditionalHealthEndpointPathsConfiguration.class);
				assertThat(context).hasNotFailed();
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
