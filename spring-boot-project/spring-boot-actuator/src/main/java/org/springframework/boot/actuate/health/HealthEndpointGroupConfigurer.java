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

import org.springframework.boot.actuate.health.HealthEndpointGroup.Show;

/**
 * A configurer for customizing an {@link HealthEndpointGroup} being built.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public interface HealthEndpointGroupConfigurer {

	/**
	 * Configure the indicator endpoint ids to include in this group.
	 * @param indicators the indicator endpoint ids
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer include(String... indicators);

	/**
	 * Configure the indicator endpoint ids to exclude from this group.
	 * @param indicators the indicator endpoint ids
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer exclude(String... indicators);

	/**
	 * Configure the {@link StatusAggregator} to use for this group.
	 * <p>
	 * If none set, this will default to the globally configured {@link StatusAggregator}.
	 * @param statusAggregator the status aggregator
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer statusAggregator(StatusAggregator statusAggregator);

	/**
	 * Configure the {@link HttpCodeStatusMapper} to use for this group.
	 * <p>
	 * If none set, this will default to the globally configured
	 * {@link HttpCodeStatusMapper}.
	 * @param httpCodeStatusMapper the status code mapper
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer httpCodeStatusMapper(HttpCodeStatusMapper httpCodeStatusMapper);

	/**
	 * Configure the {@link Show visibility option} for showing components of this group.
	 * @param showComponents the components visibility
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer showComponents(Show showComponents);

	/**
	 * Configure the {@link Show visibility option} for showing details of this group.
	 * @param showDetails the details visibility
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer showDetails(Show showDetails);

	/**
	 * Configure roles used to determine whether or not a user is authorized to be shown
	 * details.
	 * @param roles the roles
	 * @return the configurer instance
	 */
	HealthEndpointGroupConfigurer roles(String... roles);

}
