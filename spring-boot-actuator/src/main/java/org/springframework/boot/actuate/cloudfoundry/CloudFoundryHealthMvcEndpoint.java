/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;

/**
 * Extension of {@link HealthMvcEndpoint} for Cloud Foundry. Since security for Cloud
 * Foundry actuators is already handled by the {@link CloudFoundrySecurityInterceptor},
 * this endpoint skips the additional security checks done by the regular
 * {@link HealthMvcEndpoint}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryHealthMvcEndpoint extends HealthMvcEndpoint {

	CloudFoundryHealthMvcEndpoint(HealthEndpoint delegate) {
		super(delegate);
	}

	@Override
	protected boolean exposeHealthDetails(HttpServletRequest request,
			Principal principal) {
		return true;
	}

}
