/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.boot.actuate.endpoint.ApiVersion;

/**
 * A {@link HealthComponent} that represents the overall system health and the available
 * groups.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public final class SystemHealth extends CompositeHealth {

	private final Set<String> groups;

	/**
	 * Constructs a new SystemHealth object with the specified API version, status,
	 * instances, and groups.
	 * @param apiVersion the API version of the system health
	 * @param status the status of the system health
	 * @param instances a map of health components associated with their names
	 * @param groups a set of groups associated with the system health
	 */
	SystemHealth(ApiVersion apiVersion, Status status, Map<String, HealthComponent> instances, Set<String> groups) {
		super(apiVersion, status, instances);
		this.groups = (groups != null) ? new TreeSet<>(groups) : null;
	}

	/**
	 * Returns the set of groups associated with the SystemHealth.
	 * @return the set of groups, or an empty set if no groups are associated
	 */
	@JsonInclude(Include.NON_EMPTY)
	public Set<String> getGroups() {
		return this.groups;
	}

}
