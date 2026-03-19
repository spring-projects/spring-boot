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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.util.Assert;

/**
 * Provides health information for a gRPC server that can be used to update a
 * {@link HealthStatusManager}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public class GrpcServerHealth {

	private HealthContributorRegistry registry;

	private @Nullable ReactiveHealthContributorRegistry fallbackRegistry;

	private HealthCheckedGrpcComponents components;

	/**
	 * Create a new {@link GrpcServerHealth} instance.
	 * @param registry the health contributor registry
	 * @param fallbackRegistry the fallback registry or {@code null}
	 * @param components the components used to provide the server health
	 */
	public GrpcServerHealth(HealthContributorRegistry registry,
			@Nullable ReactiveHealthContributorRegistry fallbackRegistry, HealthCheckedGrpcComponents components) {
		Assert.notNull(registry, "'registry' must not be null");
		Assert.notNull(components, "'components' must not be null");
		this.registry = registry;
		this.fallbackRegistry = fallbackRegistry;
		this.components = components;
	}

	public void update(HealthStatusManager manager) {
		update(manager::setStatus);
	}

	public void update(BiConsumer<String, ServingStatus> updator) {
		Cache cache = new Cache();
		HealthCheckedGrpcComponent serverComponent = this.components.getServer();
		if (serverComponent != null) {
			updator.accept("", getServingStatus(cache, serverComponent));
		}
		for (String serviceName : this.components.getServiceNames()) {
			HealthCheckedGrpcComponent serviceComponent = this.components.getService(serviceName);
			if (!serviceName.isEmpty() && serviceComponent != null) {
				updator.accept(serviceName, getServingStatus(cache, serviceComponent));
			}
		}
	}

	private ServingStatus getServingStatus(Cache cache, HealthCheckedGrpcComponent component) {
		Set<Status> statuses = new LinkedHashSet<>();
		collectStatuses(cache, component, statuses, this.registry, "");
		if (this.fallbackRegistry != null) {
			collectStatuses(cache, component, statuses, this.fallbackRegistry.asHealthContributors(), "");
		}
		Status status = component.getStatusAggregator().getAggregateStatus(statuses);
		return component.getStatusMapper().getServingStatus(status);
	}

	private void collectStatuses(Cache cache, HealthCheckedGrpcComponent component, Set<Status> statuses,
			HealthContributors contributors, String prefix) {
		for (HealthContributors.Entry entry : contributors) {
			String name = (prefix.isEmpty()) ? entry.name() : prefix + "/" + entry.name();
			if (entry.contributor() instanceof CompositeHealthContributor composite) {
				collectStatuses(cache, component, statuses, composite, name);
			}
			else if (component.isMember(name)) {
				Health health = cache.getHealth(name, (HealthIndicator) entry.contributor());
				if (health != null) {
					statuses.add(health.getStatus());
				}
			}
		}
	}

	class Cache {

		private final Map<String, Health> health = new HashMap<>();

		Health getHealth(String name, HealthIndicator indicator) {
			return this.health.computeIfAbsent(name, (key) -> indicator.health(false));
		}

	}

}
