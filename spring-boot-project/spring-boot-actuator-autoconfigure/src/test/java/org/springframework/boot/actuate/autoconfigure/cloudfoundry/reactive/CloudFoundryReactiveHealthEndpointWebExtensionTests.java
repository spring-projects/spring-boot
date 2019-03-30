/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.time.Duration;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryReactiveHealthEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryReactiveHealthEndpointWebExtensionTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withPropertyValues("VCAP_APPLICATION={}")
			.withConfiguration(AutoConfigurations.of(
					ReactiveSecurityAutoConfiguration.class,
					ReactiveUserDetailsServiceAutoConfiguration.class,
					WebFluxAutoConfiguration.class, JacksonAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class,
					ReactiveCloudFoundryActuatorAutoConfigurationTests.WebClientCustomizerConfig.class,
					WebClientAutoConfiguration.class,
					ManagementContextAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					HealthIndicatorAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class,
					ReactiveCloudFoundryActuatorAutoConfiguration.class));

	@Test
	public void healthDetailsAlwaysPresent() {
		this.contextRunner.run((context) -> {
			CloudFoundryReactiveHealthEndpointWebExtension extension = context
					.getBean(CloudFoundryReactiveHealthEndpointWebExtension.class);
			assertThat(extension.health().block(Duration.ofSeconds(30)).getBody()
					.getDetails()).isNotEmpty();
		});
	}

}
