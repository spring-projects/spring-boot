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

package smoketest.actuator.extension;

import java.util.Set;

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
	public ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			Builder message) {
		if (exposures.contains(EndpointExposure.WEB) && this.filter.match(endpointId)) {
			return ConditionOutcome.match(message.because("marked as exposed by a my extension '"
					+ MyExtensionEndpointFilter.PROPERTY_PREFIX + "' property"));
		}
		return null;

	}

}
