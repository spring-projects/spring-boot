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

package org.springframework.boot.grpc.server.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerHealth}.
 *
 * @author Phillip Webb
 */
class GrpcServerHealthTests {

	private static final HealthIndicator UP = () -> Health.up().build();

	private static final HealthIndicator DOWN = () -> Health.down().build();

	private static final ReactiveHealthIndicator REACTIVE_DOWN = () -> Mono.just(Health.down().build());

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenRegistryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GrpcServerHealth(null, null, mock()))
			.withMessage("'registry' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenComponentsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GrpcServerHealth(mock(), null, null))
			.withMessage("'components' must not be null");
	}

	@Test
	void updateWhenHasServerComponentAndUp() {
		assertThat(updateWithServerComponent("test", UP)).containsExactly(entry("", ServingStatus.SERVING));
	}

	@Test
	void updateWhenHasServerComponentAndDown() {
		assertThat(updateWithServerComponent("test", DOWN)).containsExactly(entry("", ServingStatus.NOT_SERVING));
	}

	@Test
	void updateWhenHasServerComponentAndMissing() {
		assertThat(updateWithServerComponent("other", UP)).containsExactly(entry("", ServingStatus.UNKNOWN));
	}

	private Map<String, ServingStatus> updateWithServerComponent(String indicatorName, HealthIndicator indicator) {
		HealthCheckedGrpcComponent server = new TestHealthCheckedGrpcComponent(Set.of("test"));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(server, Collections.emptyMap());
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor(indicatorName, indicator);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		return result;
	}

	@Test
	void updateWhenHasServices() {
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one", new TestHealthCheckedGrpcComponent(Set.of("up")));
		services.put("two", new TestHealthCheckedGrpcComponent(Set.of("down")));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor("up", UP);
		registry.registerContributor("down", DOWN);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.SERVING),
				entry("two", ServingStatus.NOT_SERVING));
	}

	@Test
	void updateUsesCache() {
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one", new TestHealthCheckedGrpcComponent(Set.of("test")));
		services.put("two", new TestHealthCheckedGrpcComponent(Set.of("test")));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		HealthIndicator contributor = mock();
		given(contributor.health(false)).willReturn(Health.up().build(), Health.down().build());
		registry.registerContributor("test", contributor);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.SERVING), entry("two", ServingStatus.SERVING));
	}

	@Test
	void updateWhenHasEmptyNamedServicesDoesNotIncludeIt() {
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("", new TestHealthCheckedGrpcComponent(Set.of("up")));
		services.put("one", new TestHealthCheckedGrpcComponent(Set.of("up")));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor("up", UP);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.SERVING));
	}

	@Test
	void updateWhenHasFallbackRegistry() {
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one", new TestHealthCheckedGrpcComponent(Set.of("1")));
		services.put("two", new TestHealthCheckedGrpcComponent(Set.of("2")));
		services.put("three", new TestHealthCheckedGrpcComponent(Set.of("3")));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		DefaultReactiveHealthContributorRegistry fallbackRegistry = new DefaultReactiveHealthContributorRegistry();
		registry.registerContributor("1", UP);
		registry.registerContributor("2", UP);
		fallbackRegistry.registerContributor("2", REACTIVE_DOWN);
		fallbackRegistry.registerContributor("3", REACTIVE_DOWN);
		GrpcServerHealth health = new GrpcServerHealth(registry, fallbackRegistry, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.SERVING), entry("two", ServingStatus.SERVING),
				entry("three", ServingStatus.NOT_SERVING));
	}

	@Test
	void updateWhenHasCompositeContributor() {
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one", new TestHealthCheckedGrpcComponent(Set.of("dbs", "dbs/db1", "dbs/db2")));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		HealthContributor contributor = CompositeHealthContributor.fromMap(Map.of("db1", UP, "db2", DOWN));
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor("dbs", contributor);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.NOT_SERVING));
	}

	@Test
	void updateFiltersIndicatorsAndCompositeParentByName() {
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one", new TestHealthCheckedGrpcComponent(Set.of("dbs", "dbs/db1")));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		HealthContributor contributor = CompositeHealthContributor.fromMap(Map.of("db1", UP, "db2", DOWN));
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor("dbs", contributor);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.SERVING));
	}

	@Test
	@SuppressWarnings("unchecked")
	void updateCallsStatusAggregator() {
		StatusAggregator statusAggregator = mock();
		given(statusAggregator.getAggregateStatus((Set<Status>) any(Set.class))).willReturn(Status.DOWN);
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one",
				new TestHealthCheckedGrpcComponent(Set.of("up"), statusAggregator, StatusMapper.getDefault()));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor("up", UP);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.NOT_SERVING));
		then(statusAggregator).should().getAggregateStatus(Set.of(Status.UP));
	}

	@Test
	void updateCallsStatusMapper() {
		StatusMapper statusMapper = mock();
		given(statusMapper.getServingStatus(Status.UP)).willReturn(ServingStatus.UNRECOGNIZED);
		Map<String, HealthCheckedGrpcComponent> services = new LinkedHashMap<>();
		services.put("one",
				new TestHealthCheckedGrpcComponent(Set.of("up"), StatusAggregator.getDefault(), statusMapper));
		HealthCheckedGrpcComponents components = new TestHealthCheckedGrpcComponents(null, services);
		DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();
		registry.registerContributor("up", UP);
		GrpcServerHealth health = new GrpcServerHealth(registry, null, components);
		Map<String, ServingStatus> result = new LinkedHashMap<>();
		health.update(result::put);
		assertThat(result).containsExactly(entry("one", ServingStatus.UNRECOGNIZED));
		then(statusMapper).should().getServingStatus(Status.UP);
	}

	static class TestHealthCheckedGrpcComponents implements HealthCheckedGrpcComponents {

		private final @Nullable HealthCheckedGrpcComponent server;

		private final Map<String, HealthCheckedGrpcComponent> services;

		TestHealthCheckedGrpcComponents(@Nullable HealthCheckedGrpcComponent server,
				Map<String, HealthCheckedGrpcComponent> services) {
			this.server = server;
			this.services = services;
		}

		@Override
		public @Nullable HealthCheckedGrpcComponent getServer() {
			return this.server;
		}

		@Override
		public Set<String> getServiceNames() {
			return this.services.keySet();
		}

		@Override
		public @Nullable HealthCheckedGrpcComponent getService(String serviceName) {
			return this.services.get(serviceName);
		}

	}

	private static class TestHealthCheckedGrpcComponent implements HealthCheckedGrpcComponent {

		private final Set<String> members;

		private final StatusAggregator statusAggregator;

		private final StatusMapper statusMapper;

		TestHealthCheckedGrpcComponent(Set<String> members) {
			this(members, StatusAggregator.getDefault(), StatusMapper.getDefault());
		}

		TestHealthCheckedGrpcComponent(Set<String> members, StatusAggregator statusAggregator,
				StatusMapper statusMapper) {
			this.members = members;
			this.statusAggregator = statusAggregator;
			this.statusMapper = statusMapper;
		}

		@Override
		public boolean isMember(String healthContributorName) {
			return this.members.contains(healthContributorName);
		}

		@Override
		public StatusAggregator getStatusAggregator() {
			return this.statusAggregator;
		}

		@Override
		public StatusMapper getStatusMapper() {
			return this.statusMapper;
		}

	}

}
