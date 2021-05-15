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

import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * A collection of {@link HealthEndpointGroup groups} for use with a health endpoint.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public interface HealthEndpointGroups {

	/**
	 * Return the primary group used by the endpoint.
	 * @return the primary group (never {@code null})
	 */
	HealthEndpointGroup getPrimary();

	/**
	 * Return the names of any additional groups.
	 * @return the additional group names
	 */
	Set<String> getNames();

	/**
	 * Return the group with the specified name or {@code null} if the name is not known.
	 * @param name the name of the group
	 * @return the {@link HealthEndpointGroup} or {@code null}
	 */
	HealthEndpointGroup get(String name);

	/**
	 * Factory method to create a {@link HealthEndpointGroups} instance.
	 * @param primary the primary group
	 * @param additional the additional groups
	 * @return a new {@link HealthEndpointGroups} instance
	 */
	static HealthEndpointGroups of(HealthEndpointGroup primary, Map<String, HealthEndpointGroup> additional) {
		Assert.notNull(primary, "Primary must not be null");
		Assert.notNull(additional, "Additional must not be null");
		return new HealthEndpointGroups() {

			@Override
			public HealthEndpointGroup getPrimary() {
				return primary;
			}

			@Override
			public Set<String> getNames() {
				return additional.keySet();
			}

			@Override
			public HealthEndpointGroup get(String name) {
				return additional.get(name);
			}

		};
	}

}
