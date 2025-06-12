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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the {@link HealthEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
@ImportRuntimeHints(HealthEndpointWebExtensionRuntimeHints.class)
public class HealthEndpointWebExtension extends HealthEndpointSupport<Health, HealthDescriptor> {

	/**
	 * Create a new {@link HealthEndpointWebExtension} instance.
	 * @param registry the health contributor registry
	 * @param fallbackRegistry the fallback registry or {@code null}
	 * @param groups the health endpoint groups
	 * @param slowContributorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 * @since 4.0.0
	 */
	public HealthEndpointWebExtension(HealthContributorRegistry registry,
			ReactiveHealthContributorRegistry fallbackRegistry, HealthEndpointGroups groups,
			Duration slowContributorLoggingThreshold) {
		super(Contributor.blocking(registry, fallbackRegistry), groups, slowContributorLoggingThreshold);
	}

	@ReadOperation
	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion, WebServerNamespace serverNamespace,
			SecurityContext securityContext) {
		return health(apiVersion, serverNamespace, securityContext, false, EMPTY_PATH);
	}

	@ReadOperation
	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion, WebServerNamespace serverNamespace,
			SecurityContext securityContext, @Selector(match = Match.ALL_REMAINING) String... path) {
		return health(apiVersion, serverNamespace, securityContext, false, path);
	}

	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion, WebServerNamespace serverNamespace,
			SecurityContext securityContext, boolean showAll, String... path) {
		Result<HealthDescriptor> result = getResult(apiVersion, serverNamespace, securityContext, showAll, path);
		if (result == null) {
			return (Arrays.equals(path, EMPTY_PATH))
					? new WebEndpointResponse<>(IndicatedHealthDescriptor.UP, WebEndpointResponse.STATUS_OK)
					: new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
		}
		HealthDescriptor descriptor = result.descriptor();
		HealthEndpointGroup group = result.group();
		int statusCode = group.getHttpCodeStatusMapper().getStatusCode(descriptor.getStatus());
		return new WebEndpointResponse<>(descriptor, statusCode);
	}

	@Override
	protected HealthDescriptor aggregateDescriptors(ApiVersion apiVersion, Map<String, HealthDescriptor> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames) {
		return getCompositeDescriptor(apiVersion, contributions, statusAggregator, showComponents, groupNames);
	}

}
