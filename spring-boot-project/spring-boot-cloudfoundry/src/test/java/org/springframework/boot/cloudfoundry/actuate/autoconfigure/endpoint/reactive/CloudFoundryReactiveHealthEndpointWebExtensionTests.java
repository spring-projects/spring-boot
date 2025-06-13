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

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.reactive;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.health.CompositeHealthDescriptor;
import org.springframework.boot.actuate.health.HealthDescriptor;
import org.springframework.boot.actuate.health.IndicatedHealthDescriptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryReactiveHealthEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryReactiveHealthEndpointWebExtensionTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withPropertyValues("VCAP_APPLICATION={}")
		.withConfiguration(AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class,
				WebFluxAutoConfiguration.class, JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				CloudFoundryReactiveActuatorAutoConfigurationTests.WebClientCustomizerConfig.class,
				WebClientAutoConfiguration.class, ManagementContextAutoConfiguration.class,
				EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, CloudFoundryReactiveActuatorAutoConfiguration.class))
		.withUserConfiguration(TestHealthIndicator.class, UserDetailsServiceConfiguration.class);

	@Test
	void healthComponentsAlwaysPresent() {
		this.contextRunner.run((context) -> {
			CloudFoundryReactiveHealthEndpointWebExtension extension = context
				.getBean(CloudFoundryReactiveHealthEndpointWebExtension.class);
			HealthDescriptor descriptor = extension.health(ApiVersion.V3).block(Duration.ofSeconds(30)).getBody();
			HealthDescriptor component = ((CompositeHealthDescriptor) descriptor).getComponents()
				.entrySet()
				.iterator()
				.next()
				.getValue();
			assertThat(((IndicatedHealthDescriptor) component).getDetails()).containsEntry("spring", "boot");
		});
	}

	private static final class TestHealthIndicator implements HealthIndicator {

		@Override
		public Health health() {
			return Health.up().withDetail("spring", "boot").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDetailsServiceConfiguration {

		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return new MapReactiveUserDetailsService(
					User.withUsername("alice").password("secret").roles("admin").build());
		}

	}

}
