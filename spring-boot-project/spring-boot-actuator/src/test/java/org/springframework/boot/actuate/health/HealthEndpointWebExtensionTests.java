/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.HealthEndpointSupport.HealthResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpointWebExtension}.
 *
 * @author Phillip Webb
 */
class HealthEndpointWebExtensionTests
		extends HealthEndpointSupportTests<HealthContributorRegistry, HealthContributor, HealthComponent> {

	@Test
	@SuppressWarnings("deprecation")
	void createWhenUsingDeprecatedConstructorThrowsException() {
		HealthEndpoint delegate = mock(HealthEndpoint.class);
		HealthWebEndpointResponseMapper responseMapper = mock(HealthWebEndpointResponseMapper.class);
		assertThatIllegalStateException().isThrownBy(() -> new HealthEndpointWebExtension(delegate, responseMapper))
				.withMessage("Unable to create class org.springframework.boot.actuate."
						+ "health.HealthEndpointWebExtension using deprecated constructor");
	}

	@Test
	void healthReturnsSystemHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		WebEndpointResponse<HealthComponent> response = create(this.registry, this.groups).health(ApiVersion.LATEST,
				SecurityContext.NONE);
		HealthComponent health = response.getBody();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(SystemHealth.class);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsHttp404() {
		this.registry.registerContributor("test", createContributor(this.up));
		WebEndpointResponse<HealthComponent> response = create(this.registry, this.groups).health(ApiVersion.LATEST,
				SecurityContext.NONE, "missing");
		assertThat(response.getBody()).isNull();
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		WebEndpointResponse<HealthComponent> response = create(this.registry, this.groups).health(ApiVersion.LATEST,
				SecurityContext.NONE, "test");
		assertThat(response.getBody()).isEqualTo(this.up);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Override
	protected HealthEndpointWebExtension create(HealthContributorRegistry registry, HealthEndpointGroups groups) {
		return new HealthEndpointWebExtension(registry, groups);
	}

	@Override
	protected HealthContributorRegistry createRegistry() {
		return new DefaultHealthContributorRegistry();
	}

	@Override
	protected HealthContributor createContributor(Health health) {
		return (HealthIndicator) () -> health;
	}

	@Override
	protected HealthContributor createCompositeContributor(Map<String, HealthContributor> contributors) {
		return CompositeHealthContributor.fromMap(contributors);
	}

	@Override
	protected HealthComponent getHealth(HealthResult<HealthComponent> result) {
		return result.getHealth();
	}

}
