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

package org.springframework.boot.actuate.health;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.HealthEndpointSupport.Result;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpointWebExtension}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class HealthEndpointWebExtensionTests extends
		HealthEndpointSupportTests<HealthEndpointWebExtension, Health, HealthDescriptor, HealthContributorRegistry, HealthContributor> {

	@Test
	void healthReturnsSystemHealth() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<HealthDescriptor> response = endpoint.health(ApiVersion.LATEST, WebServerNamespace.SERVER,
				SecurityContext.NONE);
		HealthDescriptor descriptor = response.getBody();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor).isInstanceOf(SystemHealthDescriptor.class);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		HealthContributorRegistry registry = createRegistry(null);
		HealthEndpointGroups groups = HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap());
		HealthEndpointWebExtension endpoint = create(registry, groups);
		WebEndpointResponse<HealthDescriptor> response = endpoint.health(ApiVersion.LATEST, WebServerNamespace.SERVER,
				SecurityContext.NONE);
		assertThat(response.getStatus()).isEqualTo(200);
		HealthDescriptor descriptor = response.getBody();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor).isInstanceOf(IndicatedHealthDescriptor.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsHttp404() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<HealthDescriptor> response = endpoint.health(ApiVersion.LATEST, WebServerNamespace.SERVER,
				SecurityContext.NONE, "missing");
		assertThat(response.getBody()).isNull();
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<HealthDescriptor> response = endpoint.health(ApiVersion.LATEST, WebServerNamespace.SERVER,
				SecurityContext.NONE, "test");
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) response.getBody();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Override
	protected HealthEndpointWebExtension create(HealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		return new HealthEndpointWebExtension(registry, null, groups, slowIndicatorLoggingThreshold);
	}

	@Override
	protected HealthContributorRegistry createRegistry(
			Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
		return new DefaultHealthContributorRegistry(Collections.emptyList(), initialRegistrations);
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
	protected HealthDescriptor getDescriptor(Result<HealthDescriptor> result) {
		return result.descriptor();
	}

}
