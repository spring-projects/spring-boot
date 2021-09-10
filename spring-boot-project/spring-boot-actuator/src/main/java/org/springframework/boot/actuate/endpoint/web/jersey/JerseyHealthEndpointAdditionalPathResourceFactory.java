/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Set;

import org.glassfish.jersey.server.model.Resource;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
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
public class JerseyHealthEndpointAdditionalPathResourceFactory extends JerseyEndpointResourceFactory {

	private final Set<HealthEndpointGroup> groups;

	private final WebServerNamespace serverNamespace;

	public JerseyHealthEndpointAdditionalPathResourceFactory(WebServerNamespace serverNamespace,
			HealthEndpointGroups groups) {
		this.serverNamespace = serverNamespace;
		this.groups = groups.getAllWithAdditionalPath(serverNamespace);
	}

	@Override
	protected Resource createResource(EndpointMapping endpointMapping, WebOperation operation) {
		WebOperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		String matchAllRemainingPathSegmentsVariable = requestPredicate.getMatchAllRemainingPathSegmentsVariable();
		if (matchAllRemainingPathSegmentsVariable != null) {
			for (HealthEndpointGroup group : this.groups) {
				AdditionalHealthEndpointPath additionalPath = group.getAdditionalPath();
				if (additionalPath != null) {
					return getResource(endpointMapping, operation, requestPredicate, additionalPath.getValue(),
							this.serverNamespace, (data, pathSegmentsVariable) -> data.getUriInfo().getPath());
				}
			}
		}
		return null;
	}

}
