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

package org.springframework.boot.actuate.endpoint.web.jersey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.glassfish.jersey.server.model.Resource;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;

/**
 * A factory for creating Jersey {@link Resource Resources} for health groups with
 * additional path.
 *
 * @author Madhura Bhave
 * @since 2.6.0
 */
public final class JerseyHealthEndpointAdditionalPathResourceFactory {

	private final JerseyEndpointResourceFactory delegate = new JerseyEndpointResourceFactory();

	private final Set<HealthEndpointGroup> groups;

	private final WebServerNamespace serverNamespace;

	/**
     * Constructs a new JerseyHealthEndpointAdditionalPathResourceFactory with the specified serverNamespace and groups.
     * 
     * @param serverNamespace the WebServerNamespace to be used
     * @param groups the HealthEndpointGroups containing additional paths for the serverNamespace
     */
    public JerseyHealthEndpointAdditionalPathResourceFactory(WebServerNamespace serverNamespace,
			HealthEndpointGroups groups) {
		this.serverNamespace = serverNamespace;
		this.groups = groups.getAllWithAdditionalPath(serverNamespace);
	}

	/**
     * Creates a collection of resources for the given endpoint mapping and list of exposable web endpoints.
     * 
     * @param endpointMapping the endpoint mapping to use for creating the resources
     * @param endpoints the list of exposable web endpoints
     * @return a collection of resources created for the given endpoint mapping and list of exposable web endpoints
     */
    public Collection<Resource> createEndpointResources(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints) {
		return endpoints.stream()
			.flatMap((endpoint) -> endpoint.getOperations().stream())
			.flatMap((operation) -> createResources(endpointMapping, operation))
			.toList();
	}

	/**
     * Creates a stream of resources based on the given endpoint mapping and web operation.
     * 
     * @param endpointMapping the endpoint mapping to create resources for
     * @param operation the web operation to create resources for
     * @return a stream of resources
     */
    private Stream<Resource> createResources(EndpointMapping endpointMapping, WebOperation operation) {
		WebOperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		String matchAllRemainingPathSegmentsVariable = requestPredicate.getMatchAllRemainingPathSegmentsVariable();
		if (matchAllRemainingPathSegmentsVariable != null) {
			List<Resource> resources = new ArrayList<>();
			for (HealthEndpointGroup group : this.groups) {
				AdditionalHealthEndpointPath additionalPath = group.getAdditionalPath();
				if (additionalPath != null) {
					resources.add(this.delegate.getResource(endpointMapping, operation, requestPredicate,
							additionalPath.getValue(), this.serverNamespace,
							(data, pathSegmentsVariable) -> data.getUriInfo().getPath()));
				}
			}
			return resources.stream();
		}
		return Stream.empty();
	}

}
