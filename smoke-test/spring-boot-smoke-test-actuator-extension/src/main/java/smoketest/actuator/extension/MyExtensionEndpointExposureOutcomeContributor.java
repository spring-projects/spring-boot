/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.EndpointExposureOutcomeContributor;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.core.env.Environment;

class MyExtensionEndpointExposureOutcomeContributor implements EndpointExposureOutcomeContributor {

	private final MyExtensionEndpointFilter filter;

	MyExtensionEndpointExposureOutcomeContributor(Environment environment) {
		this.filter = new MyExtensionEndpointFilter(environment);
	}

	@Override
	public @Nullable ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			Builder message) {
		if (exposures.contains(EndpointExposure.WEB) && this.filter.match(endpointId)) {
			return ConditionOutcome.match(message.because("marked as exposed by a my extension '"
					+ MyExtensionEndpointFilter.PROPERTY_PREFIX + "' property"));
		}
		return null;

	}

}
