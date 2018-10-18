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

package org.springframework.boot.actuate.autoconfigure.health;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthWebEndpointResponseMapper;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpointAutoConfiguration} in a servlet environment.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class HealthEndpointWebExtensionTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(HealthIndicatorsConfiguration.class).withConfiguration(
					AutoConfigurations.of(HealthIndicatorAutoConfiguration.class,
							HealthEndpointAutoConfiguration.class));

	@Test
	public void runShouldCreateExtensionBeans() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(HealthEndpointWebExtension.class));
	}

	@Test
	public void runWhenHealthEndpointIsDisabledShouldNotCreateExtensionBeans() {
		this.contextRunner.withPropertyValues("management.endpoint.health.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(HealthEndpointWebExtension.class));
	}

	@Test
	public void runWithCustomHealthMappingShouldMapStatusCode() {
		this.contextRunner
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.run((context) -> {
					Object extension = context.getBean(HealthEndpointWebExtension.class);
					HealthWebEndpointResponseMapper responseMapper = (HealthWebEndpointResponseMapper) ReflectionTestUtils
							.getField(extension, "responseMapper");
					Class<SecurityContext> securityContext = SecurityContext.class;
					assertThat(responseMapper
							.map(Health.down().build(), mock(securityContext))
							.getStatus()).isEqualTo(503);
					assertThat(responseMapper.map(Health.status("OUT_OF_SERVICE").build(),
							mock(securityContext)).getStatus()).isEqualTo(503);
					assertThat(responseMapper
							.map(Health.status("CUSTOM").build(), mock(securityContext))
							.getStatus()).isEqualTo(500);
				});
	}

	@Test
	public void unauthenticatedUsersAreNotShownDetailsByDefault() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension extension = context
					.getBean(HealthEndpointWebExtension.class);
			assertThat(
					extension.health(mock(SecurityContext.class)).getBody().getDetails())
							.isEmpty();
		});
	}

	@Test
	public void authenticatedUsersAreNotShownDetailsByDefault() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension extension = context
					.getBean(HealthEndpointWebExtension.class);
			SecurityContext securityContext = mock(SecurityContext.class);
			given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
			assertThat(extension.health(securityContext).getBody().getDetails())
					.isEmpty();
		});
	}

	@Test
	public void authenticatedUsersWhenAuthorizedCanBeShownDetails() {
		this.contextRunner
				.withPropertyValues(
						"management.endpoint.health.show-details=when-authorized")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					assertThat(extension.health(securityContext).getBody().getDetails())
							.isNotEmpty();
				});
	}

	@Test
	public void unauthenticatedUsersCanBeShownDetails() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=always")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertThat(extension.health(null).getBody().getDetails())
							.isNotEmpty();
				});
	}

	@Test
	public void detailsCanBeHiddenFromAuthenticatedUsers() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=never")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertThat(extension.health(mock(SecurityContext.class)).getBody()
							.getDetails()).isEmpty();
				});
	}

	@Test
	public void detailsCanBeHiddenFromUnauthorizedUsers() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ACTUATOR").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ACTUATOR")).willReturn(false);
					assertThat(extension.health(securityContext).getBody().getDetails())
							.isEmpty();
				});
	}

	@Test
	public void detailsCanBeShownToAuthorizedUsers() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ACTUATOR").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ACTUATOR")).willReturn(true);
					assertThat(extension.health(securityContext).getBody().getDetails())
							.isNotEmpty();
				});
	}

	@Test
	public void unauthenticatedUsersAreNotShownComponentByDefault() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension extension = context
					.getBean(HealthEndpointWebExtension.class);
			assertDetailsNotFound(
					extension.healthForComponent(mock(SecurityContext.class), "simple"));
		});
	}

	@Test
	public void authenticatedUsersAreNotShownComponentByDefault() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension extension = context
					.getBean(HealthEndpointWebExtension.class);
			SecurityContext securityContext = mock(SecurityContext.class);
			given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
			assertDetailsNotFound(
					extension.healthForComponent(securityContext, "simple"));
		});
	}

	@Test
	public void authenticatedUsersWhenAuthorizedCanBeShownComponent() {
		this.contextRunner
				.withPropertyValues(
						"management.endpoint.health.show-details=when-authorized")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					assertSimpleComponent(
							extension.healthForComponent(securityContext, "simple"));
				});
	}

	@Test
	public void unauthenticatedUsersCanBeShownComponent() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=always")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertSimpleComponent(extension.healthForComponent(null, "simple"));
				});
	}

	@Test
	public void componentCanBeHiddenFromAuthenticatedUsers() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=never")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertDetailsNotFound(extension
							.healthForComponent(mock(SecurityContext.class), "simple"));
				});
	}

	@Test
	public void componentCanBeHiddenFromUnauthorizedUsers() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ACTUATOR").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ACTUATOR")).willReturn(false);
					assertDetailsNotFound(
							extension.healthForComponent(securityContext, "simple"));
				});
	}

	@Test
	public void componentCanBeShownToAuthorizedUsers() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ACTUATOR").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ACTUATOR")).willReturn(true);
					assertSimpleComponent(
							extension.healthForComponent(securityContext, "simple"));
				});
	}

	@Test
	public void componentThatDoesNotExistMapTo404() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=always")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertDetailsNotFound(
							extension.healthForComponent(null, "does-not-exist"));
				});
	}

	@Test
	public void unauthenticatedUsersAreNotShownComponentInstanceByDefault() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension extension = context
					.getBean(HealthEndpointWebExtension.class);
			assertDetailsNotFound(extension.healthForComponentInstance(
					mock(SecurityContext.class), "composite", "one"));
		});
	}

	@Test
	public void authenticatedUsersAreNotShownComponentInstanceByDefault() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension extension = context
					.getBean(HealthEndpointWebExtension.class);
			SecurityContext securityContext = mock(SecurityContext.class);
			given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
			assertDetailsNotFound(extension.healthForComponentInstance(securityContext,
					"composite", "one"));
		});
	}

	@Test
	public void authenticatedUsersWhenAuthorizedCanBeShownComponentInstance() {
		this.contextRunner
				.withPropertyValues(
						"management.endpoint.health.show-details=when-authorized")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					assertSimpleComponent(extension.healthForComponentInstance(
							securityContext, "composite", "one"));
				});
	}

	@Test
	public void unauthenticatedUsersCanBeShownComponentInstance() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=always")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertSimpleComponent(extension.healthForComponentInstance(null,
							"composite", "one"));
				});
	}

	@Test
	public void componentInstanceCanBeHiddenFromAuthenticatedUsers() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=never")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertDetailsNotFound(extension.healthForComponentInstance(
							mock(SecurityContext.class), "composite", "one"));
				});
	}

	@Test
	public void componentInstanceCanBeHiddenFromUnauthorizedUsers() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ACTUATOR").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ACTUATOR")).willReturn(false);
					assertDetailsNotFound(extension.healthForComponentInstance(
							securityContext, "composite", "one"));
				});
	}

	@Test
	public void componentInstanceCanBeShownToAuthorizedUsers() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ACTUATOR").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ACTUATOR")).willReturn(true);
					assertSimpleComponent(extension.healthForComponentInstance(
							securityContext, "composite", "one"));
				});
	}

	@Test
	public void componentInstanceThatDoesNotExistMapTo404() {
		this.contextRunner
				.withPropertyValues("management.endpoint.health.show-details=always")
				.run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					assertDetailsNotFound(extension.healthForComponentInstance(null,
							"composite", "does-not-exist"));
				});
	}

	private void assertDetailsNotFound(WebEndpointResponse<?> response) {
		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(response.getBody()).isNull();
	}

	private void assertSimpleComponent(WebEndpointResponse<Health> response) {
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.getBody().getDetails()).containsOnly(entry("counter", 42));
	}

	@Test
	public void roleCanBeCustomized() {
		this.contextRunner.withPropertyValues(
				"management.endpoint.health.show-details=when-authorized",
				"management.endpoint.health.roles=ADMIN").run((context) -> {
					HealthEndpointWebExtension extension = context
							.getBean(HealthEndpointWebExtension.class);
					SecurityContext securityContext = mock(SecurityContext.class);
					given(securityContext.getPrincipal())
							.willReturn(mock(Principal.class));
					given(securityContext.isUserInRole("ADMIN")).willReturn(true);
					assertThat(extension.health(securityContext).getBody().getDetails())
							.isNotEmpty();
				});
	}

	@Configuration
	static class HealthIndicatorsConfiguration {

		@Bean
		public HealthIndicator simpleHealthIndicator() {
			return () -> Health.up().withDetail("counter", 42).build();
		}

		@Bean
		public HealthIndicator compositeHealthIndicator() {
			Map<String, HealthIndicator> nestedIndicators = new HashMap<>();
			nestedIndicators.put("one", simpleHealthIndicator());
			nestedIndicators.put("two", () -> Health.up().build());
			return new CompositeHealthIndicator(new OrderedHealthAggregator(),
					nestedIndicators);
		}

	}

}
