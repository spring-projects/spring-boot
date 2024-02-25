/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties used to configure the health endpoint and endpoint groups.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.2.0
 */
public abstract class HealthProperties {

	@NestedConfigurationProperty
	private final Status status = new Status();

	/**
	 * When to show components. If not specified the 'show-details' setting will be used.
	 */
	private Show showComponents;

	/**
	 * Roles used to determine whether a user is authorized to be shown details. When
	 * empty, all authenticated users are authorized.
	 */
	private Set<String> roles = new HashSet<>();

	/**
	 * Returns the status of the HealthProperties.
	 * @return the status of the HealthProperties
	 */
	public Status getStatus() {
		return this.status;
	}

	/**
	 * Returns the show components of the HealthProperties.
	 * @return the show components of the HealthProperties
	 */
	public Show getShowComponents() {
		return this.showComponents;
	}

	/**
	 * Sets the showComponents property of the HealthProperties class.
	 * @param showComponents the Show object to set as the showComponents property
	 */
	public void setShowComponents(Show showComponents) {
		this.showComponents = showComponents;
	}

	/**
	 * Retrieves the details of a show.
	 * @return the show details
	 */
	public abstract Show getShowDetails();

	/**
	 * Returns the set of roles associated with the HealthProperties object.
	 * @return the set of roles
	 */
	public Set<String> getRoles() {
		return this.roles;
	}

	/**
	 * Sets the roles for the HealthProperties.
	 * @param roles the set of roles to be set
	 */
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	/**
	 * Status properties for the group.
	 */
	public static class Status {

		/**
		 * Comma-separated list of health statuses in order of severity.
		 */
		private List<String> order = new ArrayList<>();

		/**
		 * Mapping of health statuses to HTTP status codes. By default, registered health
		 * statuses map to sensible defaults (for example, UP maps to 200).
		 */
		private final Map<String, Integer> httpMapping = new HashMap<>();

		/**
		 * Returns the order list.
		 * @return the order list
		 */
		public List<String> getOrder() {
			return this.order;
		}

		/**
		 * Sets the order of the status.
		 * @param statusOrder the list of status order
		 */
		public void setOrder(List<String> statusOrder) {
			if (statusOrder != null && !statusOrder.isEmpty()) {
				this.order = statusOrder;
			}
		}

		/**
		 * Returns the HTTP mapping as a Map object.
		 * @return the HTTP mapping as a Map object.
		 */
		public Map<String, Integer> getHttpMapping() {
			return this.httpMapping;
		}

	}

}
