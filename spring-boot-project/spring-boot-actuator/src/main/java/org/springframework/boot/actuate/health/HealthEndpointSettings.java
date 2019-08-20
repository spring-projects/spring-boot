/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.actuate.endpoint.SecurityContext;

/**
 * Setting for a {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public interface HealthEndpointSettings {

	/**
	 * Returns if {@link Health#getDetails() health details} should be included in the
	 * response.
	 * @param securityContext the endpoint security context
	 * @return {@code true} to included details or {@code false} to hide them
	 */
	boolean includeDetails(SecurityContext securityContext);

	/**
	 * Returns the status agreggator that should be used for the endpoint.
	 * @return the status aggregator
	 */
	StatusAggregator getStatusAggregator();

	/**
	 * Returns the {@link HttpCodeStatusMapper} that should be used for the endpoint.
	 * @return the HTTP code status mapper
	 */
	HttpCodeStatusMapper getHttpCodeStatusMapper();

}
