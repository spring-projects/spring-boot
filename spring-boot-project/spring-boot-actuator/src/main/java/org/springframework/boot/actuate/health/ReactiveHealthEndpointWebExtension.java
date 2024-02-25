/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Reactive {@link EndpointWebExtension @EndpointWebExtension} for the
 * {@link HealthEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
@ImportRuntimeHints(HealthEndpointWebExtensionRuntimeHints.class)
public class ReactiveHealthEndpointWebExtension
		extends HealthEndpointSupport<ReactiveHealthContributor, Mono<? extends HealthComponent>> {

	private static final String[] NO_PATH = {};

	/**
	 * Create a new {@link ReactiveHealthEndpointWebExtension} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 * @since 2.6.9
	 */
	public ReactiveHealthEndpointWebExtension(ReactiveHealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		super(registry, groups, slowIndicatorLoggingThreshold);
	}

	/**
	 * Retrieves the health status of the web endpoint.
	 * @param apiVersion The version of the API.
	 * @param serverNamespace The namespace of the web server.
	 * @param securityContext The security context for the request.
	 * @return A Mono containing the health status of the web endpoint.
	 */
	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			WebServerNamespace serverNamespace, SecurityContext securityContext) {
		return health(apiVersion, serverNamespace, securityContext, false, NO_PATH);
	}

	/**
	 * Retrieves the health status of the web endpoint.
	 * @param apiVersion The version of the API.
	 * @param serverNamespace The namespace of the web server.
	 * @param securityContext The security context.
	 * @param path The remaining path segments.
	 * @return A Mono containing the health status of the web endpoint.
	 */
	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			WebServerNamespace serverNamespace, SecurityContext securityContext,
			@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(apiVersion, serverNamespace, securityContext, false, path);
	}

	/**
	 * Retrieves the health status of the specified API version, server namespace,
	 * security context, and path.
	 * @param apiVersion The API version to retrieve health for.
	 * @param serverNamespace The server namespace to retrieve health for.
	 * @param securityContext The security context to retrieve health for.
	 * @param showAll Flag indicating whether to show all health components.
	 * @param path The path to retrieve health for.
	 * @return A Mono containing the WebEndpointResponse with the health status.
	 */
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			WebServerNamespace serverNamespace, SecurityContext securityContext, boolean showAll, String... path) {
		HealthResult<Mono<? extends HealthComponent>> result = getHealth(apiVersion, serverNamespace, securityContext,
				showAll, path);
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

	/**
	 * Retrieves the health of a given ReactiveHealthContributor.
	 * @param contributor the ReactiveHealthContributor to retrieve the health from
	 * @param includeDetails a boolean indicating whether to include detailed health
	 * information
	 * @return a Mono emitting the HealthComponent representing the health of the
	 * contributor
	 */
	@Override
	protected Mono<? extends HealthComponent> getHealth(ReactiveHealthContributor contributor, boolean includeDetails) {
		return ((ReactiveHealthIndicator) contributor).getHealth(includeDetails);
	}

	/**
	 * Aggregates the contributions from different health components and returns the
	 * composite health.
	 * @param apiVersion the API version
	 * @param contributions the map of health component contributions
	 * @param statusAggregator the status aggregator
	 * @param showComponents flag to indicate whether to show individual components in the
	 * composite health
	 * @param groupNames the set of group names to include in the composite health
	 * @return a Mono of the composite health component
	 */
	@Override
	protected Mono<? extends HealthComponent> aggregateContributions(ApiVersion apiVersion,
			Map<String, Mono<? extends HealthComponent>> contributions, StatusAggregator statusAggregator,
			boolean showComponents, Set<String> groupNames) {
		return Flux.fromIterable(contributions.entrySet())
			.flatMap(NamedHealthComponent::create)
			.collectMap(NamedHealthComponent::getName, NamedHealthComponent::getHealth)
			.map((components) -> this.getCompositeHealth(apiVersion, components, statusAggregator, showComponents,
					groupNames));
	}

	/**
	 * A named {@link HealthComponent}.
	 */
	private static final class NamedHealthComponent {

		private final String name;

		private final HealthComponent health;

		/**
		 * Constructs a new NamedHealthComponent with the given name and health component.
		 * @param pair the pair of objects containing the name and health component
		 */
		private NamedHealthComponent(Object... pair) {
			this.name = (String) pair[0];
			this.health = (HealthComponent) pair[1];
		}

		/**
		 * Returns the name of the NamedHealthComponent.
		 * @return the name of the NamedHealthComponent
		 */
		String getName() {
			return this.name;
		}

		/**
		 * Returns the health of the NamedHealthComponent.
		 * @return the health of the NamedHealthComponent
		 */
		HealthComponent getHealth() {
			return this.health;
		}

		/**
		 * Creates a Mono of NamedHealthComponent using the given entry.
		 * @param entry the entry containing the name and health component
		 * @return a Mono of NamedHealthComponent
		 */
		static Mono<NamedHealthComponent> create(Map.Entry<String, Mono<? extends HealthComponent>> entry) {
			Mono<String> name = Mono.just(entry.getKey());
			Mono<? extends HealthComponent> health = entry.getValue();
			return Mono.zip(NamedHealthComponent::new, name, health);
		}

	}

}
