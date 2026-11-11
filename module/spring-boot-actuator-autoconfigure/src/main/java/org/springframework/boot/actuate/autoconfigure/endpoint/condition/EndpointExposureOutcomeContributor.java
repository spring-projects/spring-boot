/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.core.env.Environment;

/**
 * Contributor loaded from the {@code spring.factories} file and used by
 * {@link ConditionalOnAvailableEndpoint @ConditionalOnAvailableEndpoint} to determine if
 * an endpoint is exposed. If any contributor returns a {@link ConditionOutcome#isMatch()
 * matching} {@link ConditionOutcome} then the endpoint is considered exposed.
 * <p>
 * Implementations may declare a constructor that accepts an {@link Environment} argument.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.4.0
 */
public interface EndpointExposureOutcomeContributor {

	/**
	 * Return if the given endpoint is exposed for the given set of exposure technologies.
	 * @param endpointId the endpoint ID
	 * @param exposures the exposure technologies to check
	 * @param message the condition message builder
	 * @return a {@link ConditionOutcome#isMatch() matching} {@link ConditionOutcome} if
	 * the endpoint is exposed or {@code null} if the contributor should not apply
	 */
	@Nullable ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			ConditionMessage.Builder message);

}
