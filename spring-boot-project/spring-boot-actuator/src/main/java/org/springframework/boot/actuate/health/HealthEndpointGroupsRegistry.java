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

import java.util.function.Consumer;

/**
 * Builder for an {@link HealthEndpointGroups} immutable instance.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public interface HealthEndpointGroupsRegistry extends HealthEndpointGroups {

	/**
	 * Add a new {@link HealthEndpointGroup}.
	 * @param groupName the name of the group to add
	 * @param builder the group to add
	 * @return the builder instance
	 */
	HealthEndpointGroupsRegistry add(String groupName, Consumer<HealthEndpointGroupConfigurer> builder);

	/**
	 * Remove an existing {@link HealthEndpointGroup}.
	 * @param groupName the name of the group to remove
	 * @return the builder instance
	 */
	HealthEndpointGroupsRegistry remove(String groupName);

	/**
	 * Build an immutable {@link HealthEndpointGroups}.
	 * @return the {@link HealthEndpointGroups}
	 */
	HealthEndpointGroups toGroups();

}
