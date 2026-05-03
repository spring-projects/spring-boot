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

package org.springframework.boot.health.actuate.endpoint;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointSupport.Result;
import org.springframework.boot.health.contributor.CompositeReactiveHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveHealthEndpointWebExtension}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ReactiveHealthEndpointWebExtensionTests extends
		HealthEndpointSupportTests<ReactiveHealthEndpointWebExtension, Mono<? extends Health>, Mono<? extends HealthDescriptor>, ReactiveHealthContributorRegistry, ReactiveHealthContributor> {

	@Test
	void healthReturnsSystemHealth() {
		ReactiveHealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ReactiveHealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<? extends HealthDescriptor> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE)
			.block();
		assertThat(response).isNotNull();
		HealthDescriptor descriptor = response.getBody();
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor).isInstanceOf(SystemHealthDescriptor.class);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		ReactiveHealthContributorRegistry registry = createRegistry(null);
		HealthEndpointGroups groups = HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap());
		ReactiveHealthEndpointWebExtension endpoint = create(registry, groups);
		WebEndpointResponse<? extends HealthDescriptor> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE)
			.block();
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
		HealthDescriptor descriptor = response.getBody();
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor).isInstanceOf(IndicatedHealthDescriptor.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsHttp404() {
		ReactiveHealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ReactiveHealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<? extends HealthDescriptor> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE, "missing")
			.block();
		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNull();
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		ReactiveHealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ReactiveHealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<? extends HealthDescriptor> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE, "test")
			.block();
		assertThat(response).isNotNull();
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) response.getBody();
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Override
	protected ReactiveHealthEndpointWebExtension create(ReactiveHealthContributorRegistry registry,
			HealthEndpointGroups groups, @Nullable Duration slowContributorLoggingThreshold) {
		return new ReactiveHealthEndpointWebExtension(registry, null, groups, slowContributorLoggingThreshold);
	}

	@Override
	protected ReactiveHealthContributorRegistry createRegistry(
			@Nullable Consumer<BiConsumer<String, ReactiveHealthContributor>> initialRegistrations) {
		return new DefaultReactiveHealthContributorRegistry(Collections.emptyList(), initialRegistrations);
	}

	@Override
	protected ReactiveHealthContributor createContributor(Health health) {
		return (ReactiveHealthIndicator) () -> Mono.just(health);
	}

	@Override
	protected ReactiveHealthContributor createCompositeContributor(
			Map<String, ReactiveHealthContributor> contributors) {
		return CompositeReactiveHealthContributor.fromMap(contributors);
	}

	@Override
	protected HealthDescriptor getDescriptor(Result<Mono<? extends HealthDescriptor>> result) {
		HealthDescriptor descriptor = result.descriptor().block();
		assertThat(descriptor).isNotNull();
		return descriptor;
	}

}
