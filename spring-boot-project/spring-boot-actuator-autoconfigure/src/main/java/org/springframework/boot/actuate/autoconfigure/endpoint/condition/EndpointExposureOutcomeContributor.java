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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.util.Set;

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
	ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			ConditionMessage.Builder message);

}
