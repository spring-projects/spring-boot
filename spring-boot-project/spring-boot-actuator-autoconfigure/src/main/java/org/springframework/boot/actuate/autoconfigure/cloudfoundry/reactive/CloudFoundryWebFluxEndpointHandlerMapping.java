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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.SecurityResponse;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive.CloudFoundryWebFluxEndpointHandlerMapping.CloudFoundryWebFluxEndpointHandlerMappingRuntimeHints;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.reactive.AbstractWebFluxEndpointHandlerMapping;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.server.ServerWebExchange;

/**
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available on
 * Cloud Foundry specific URLs over HTTP using Spring WebFlux.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Brian Clozel
 */
@ImportRuntimeHints(CloudFoundryWebFluxEndpointHandlerMappingRuntimeHints.class)
class CloudFoundryWebFluxEndpointHandlerMapping extends AbstractWebFluxEndpointHandlerMapping {

	private final CloudFoundrySecurityInterceptor securityInterceptor;

	private final EndpointLinksResolver linksResolver;

	private final Collection<ExposableEndpoint<?>> allEndpoints;

	CloudFoundryWebFluxEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
			CorsConfiguration corsConfiguration, CloudFoundrySecurityInterceptor securityInterceptor,
			Collection<ExposableEndpoint<?>> allEndpoints) {
		super(endpointMapping, endpoints, endpointMediaTypes, corsConfiguration, true);
		this.linksResolver = new EndpointLinksResolver(allEndpoints);
		this.allEndpoints = allEndpoints;
		this.securityInterceptor = securityInterceptor;
	}

	@Override
	protected ReactiveWebOperation wrapReactiveWebOperation(ExposableWebEndpoint endpoint, WebOperation operation,
			ReactiveWebOperation reactiveWebOperation) {
		return new SecureReactiveWebOperation(reactiveWebOperation, this.securityInterceptor, endpoint.getEndpointId());
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return new CloudFoundryLinksHandler();
	}

	Collection<ExposableEndpoint<?>> getAllEndpoints() {
		return this.allEndpoints;
	}

	class CloudFoundryLinksHandler implements LinksHandler {

		@Override
		@Reflective
		public Publisher<ResponseEntity<Object>> links(ServerWebExchange exchange) {
			ServerHttpRequest request = exchange.getRequest();
			return CloudFoundryWebFluxEndpointHandlerMapping.this.securityInterceptor.preHandle(exchange, "")
				.map((securityResponse) -> {
					if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
						return new ResponseEntity<>(securityResponse.getStatus());
					}
					AccessLevel accessLevel = exchange.getAttribute(AccessLevel.REQUEST_ATTRIBUTE);
					Map<String, Link> links = CloudFoundryWebFluxEndpointHandlerMapping.this.linksResolver
						.resolveLinks(request.getURI().toString());
					return new ResponseEntity<>(
							Collections.singletonMap("_links", getAccessibleLinks(accessLevel, links)), HttpStatus.OK);
				});
		}

		private Map<String, Link> getAccessibleLinks(AccessLevel accessLevel, Map<String, Link> links) {
			if (accessLevel == null) {
				return new LinkedHashMap<>();
			}
			return links.entrySet()
				.stream()
				.filter((entry) -> entry.getKey().equals("self") || accessLevel.isAccessAllowed(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		@Override
		public String toString() {
			return "Actuator root web endpoint";
		}

	}

	/**
	 * {@link ReactiveWebOperation} wrapper to add security.
	 */
	private static class SecureReactiveWebOperation implements ReactiveWebOperation {

		private final ReactiveWebOperation delegate;

		private final CloudFoundrySecurityInterceptor securityInterceptor;

		private final EndpointId endpointId;

		SecureReactiveWebOperation(ReactiveWebOperation delegate, CloudFoundrySecurityInterceptor securityInterceptor,
				EndpointId endpointId) {
			this.delegate = delegate;
			this.securityInterceptor = securityInterceptor;
			this.endpointId = endpointId;
		}

		@Override
		public Mono<ResponseEntity<Object>> handle(ServerWebExchange exchange, Map<String, String> body) {
			return this.securityInterceptor.preHandle(exchange, this.endpointId.toLowerCaseString())
				.flatMap((securityResponse) -> flatMapResponse(exchange, body, securityResponse));
		}

		private Mono<ResponseEntity<Object>> flatMapResponse(ServerWebExchange exchange, Map<String, String> body,
				SecurityResponse securityResponse) {
			if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
				return Mono.just(new ResponseEntity<>(securityResponse.getStatus()));
			}
			return this.delegate.handle(exchange, body);
		}

	}

	static class CloudFoundryWebFluxEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {

		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.reflectiveRegistrar.registerRuntimeHints(hints, CloudFoundryLinksHandler.class);
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), Link.class);
		}

	}

}
