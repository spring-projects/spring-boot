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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.EndpointExposureOutcomeContributor;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.core.env.Environment;

/**
 * {@link EndpointExposureOutcomeContributor} to expose {@link EndpointExposure#WEB web}
 * endpoints for Cloud Foundry.
 *
 * @author Phillip Webb
 */
class CloudFoundryEndpointExposureOutcomeContributor implements EndpointExposureOutcomeContributor {

	private static final String PROPERTY = "management.endpoints.cloud-foundry.exposure";

	private final IncludeExcludeEndpointFilter<?> filter;

	CloudFoundryEndpointExposureOutcomeContributor(Environment environment) {
		this.filter = (!CloudPlatform.CLOUD_FOUNDRY.isActive(environment)) ? null
				: new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, environment, PROPERTY, "*");
	}

	@Override
	public ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			Builder message) {
		if (exposures.contains(EndpointExposure.WEB) && this.filter != null && this.filter.match(endpointId)) {
			return ConditionOutcome.match(message.because("marked as exposed by a '" + PROPERTY + "' property"));
		}
		return null;
	}

}
