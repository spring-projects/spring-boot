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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;

/**
 * Reactive {@link EndpointWebExtension @EndpointWebExtension} for the
 * {@link HealthEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
public class ReactiveHealthEndpointWebExtension
		extends HealthEndpointSupport<ReactiveHealthContributor, Mono<? extends HealthComponent>> {

	private static final String[] NO_PATH = {};

	/**
	 * Create a new {@link ReactiveHealthEndpointWebExtension} instance.
	 * @param delegate the delegate health indicator
	 * @param responseMapper the response mapper
	 * @deprecated since 2.2.0 in favor of
	 * {@link #ReactiveHealthEndpointWebExtension(ReactiveHealthContributorRegistry, HealthEndpointGroups)}
	 */
	@Deprecated
	public ReactiveHealthEndpointWebExtension(ReactiveHealthIndicator delegate,
			HealthWebEndpointResponseMapper responseMapper) {
	}

	/**
	 * Create a new {@link ReactiveHealthEndpointWebExtension} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 */
	public ReactiveHealthEndpointWebExtension(ReactiveHealthContributorRegistry registry, HealthEndpointGroups groups) {
		super(registry, groups);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			SecurityContext securityContext) {
		return health(apiVersion, securityContext, false, NO_PATH);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			SecurityContext securityContext, @Selector(match = Match.ALL_REMAINING) String... path) {
		return health(apiVersion, securityContext, false, path);
	}

	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			SecurityContext securityContext, boolean showAll, String... path) {
		HealthResult<Mono<? extends HealthComponent>> result = getHealth(apiVersion, securityContext, showAll, path);
		if (result == null) {
			return (Arrays.equals(path, NO_PATH))
					? Mono.just(new WebEndpointResponse<>(DEFAULT_HEALTH, WebEndpointResponse.STATUS_OK))
					: Mono.just(new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND));
		}
		HealthEndpointGroup group = result.getGroup();
		return result.getHealth().map((health) -> {
			int statusCode = group.getHttpCodeStatusMapper().getStatusCode(health.getStatus());
			return new WebEndpointResponse<>(health, statusCode);
		});
	}

	@Override
	protected Mono<? extends HealthComponent> getHealth(ReactiveHealthContributor contributor, boolean includeDetails) {
		return ((ReactiveHealthIndicator) contributor).getHealth(includeDetails);
	}

	@Override
	protected Mono<? extends HealthComponent> aggregateContributions(ApiVersion apiVersion,
			Map<String, Mono<? extends HealthComponent>> contributions, StatusAggregator statusAggregator,
			boolean showComponents, Set<String> groupNames) {
		return Flux.fromIterable(contributions.entrySet()).flatMap(NamedHealthComponent::create)
				.collectMap(NamedHealthComponent::getName, NamedHealthComponent::getHealth).map((components) -> this
						.getCompositeHealth(apiVersion, components, statusAggregator, showComponents, groupNames));
	}

	/**
	 * A named {@link HealthComponent}.
	 */
	private static final class NamedHealthComponent {

		private final String name;

		private final HealthComponent health;

		private NamedHealthComponent(Object... pair) {
			this.name = (String) pair[0];
			this.health = (HealthComponent) pair[1];
		}

		String getName() {
			return this.name;
		}

		HealthComponent getHealth() {
			return this.health;
		}

		static Mono<NamedHealthComponent> create(Map.Entry<String, Mono<? extends HealthComponent>> entry) {
			Mono<String> name = Mono.just(entry.getKey());
			Mono<? extends HealthComponent> health = entry.getValue();
			return Mono.zip(NamedHealthComponent::new, name, health);
		}

	}

}
