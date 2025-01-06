/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A custom {@link HandlerMapping} that allows health groups to be mapped to an additional
 * path.
 *
 * @author Madhura Bhave
 * @since 2.6.0
 */
public class AdditionalHealthEndpointPathsWebMvcHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private final ExposableWebEndpoint healthEndpoint;

	private final Set<HealthEndpointGroup> groups;

	public AdditionalHealthEndpointPathsWebMvcHandlerMapping(ExposableWebEndpoint healthEndpoint,
			Set<HealthEndpointGroup> groups) {
		super(new EndpointMapping(""), asList(healthEndpoint), null, false);
		this.healthEndpoint = healthEndpoint;
		this.groups = groups;
	}

	private static Collection<ExposableWebEndpoint> asList(ExposableWebEndpoint healthEndpoint) {
		return (healthEndpoint != null) ? Collections.singletonList(healthEndpoint) : Collections.emptyList();
	}

	@Override
	protected void initHandlerMethods() {
		if (this.healthEndpoint == null) {
			return;
		}
		for (WebOperation operation : this.healthEndpoint.getOperations()) {
			WebOperationRequestPredicate predicate = operation.getRequestPredicate();
			String matchAllRemainingPathSegmentsVariable = predicate.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				for (HealthEndpointGroup group : this.groups) {
					AdditionalHealthEndpointPath additionalPath = group.getAdditionalPath();
					if (additionalPath != null) {
						registerMapping(this.healthEndpoint, predicate, operation, additionalPath.getValue());
					}
				}
			}
		}
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return null;
	}

}
