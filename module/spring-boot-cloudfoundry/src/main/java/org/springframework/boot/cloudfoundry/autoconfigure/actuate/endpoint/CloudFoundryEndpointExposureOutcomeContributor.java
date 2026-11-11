/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import java.util.Set;

import org.jspecify.annotations.Nullable;

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

	private final @Nullable IncludeExcludeEndpointFilter<?> filter;

	CloudFoundryEndpointExposureOutcomeContributor(Environment environment) {
		this.filter = (!CloudPlatform.CLOUD_FOUNDRY.isActive(environment)) ? null
				: new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, environment, PROPERTY, "*");
	}

	@Override
	public @Nullable ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			Builder message) {
		if (exposures.contains(EndpointExposure.WEB) && this.filter != null && this.filter.match(endpointId)) {
			return ConditionOutcome.match(message.because("marked as exposed by a '" + PROPERTY + "' property"));
		}
		return null;
	}

}
