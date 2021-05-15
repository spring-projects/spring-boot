/*
 * Copyright 2012-2020 the original author or authors.
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
 * A logical grouping of {@link HealthContributor health contributors} that can be exposed
 * by the {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public interface HealthEndpointGroup {

	/**
	 * Returns {@code true} if the given contributor is a member of this group.
	 * @param name the contributor name
	 * @return {@code true} if the contributor is a member of this group
	 */
	boolean isMember(String name);

	/**
	 * Returns if {@link CompositeHealth#getComponents() health components} should be
	 * shown in the response.
	 * @param securityContext the endpoint security context
	 * @return {@code true} to shown details or {@code false} to hide them
	 */
	boolean showComponents(SecurityContext securityContext);

	/**
	 * Returns if {@link Health#getDetails() health details} should be shown in the
	 * response.
	 * @param securityContext the endpoint security context
	 * @return {@code true} to shown details or {@code false} to hide them
	 */
	boolean showDetails(SecurityContext securityContext);

	/**
	 * Returns the status aggregator that should be used for this group.
	 * @return the status aggregator for this group
	 */
	StatusAggregator getStatusAggregator();

	/**
	 * Returns the {@link HttpCodeStatusMapper} that should be used for this group.
	 * @return the HTTP code status mapper
	 */
	HttpCodeStatusMapper getHttpCodeStatusMapper();

}
