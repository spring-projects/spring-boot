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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@ConfigurationProperties("management.endpoint.health")
public class HealthEndpointProperties {

	private final Status status = new Status();

	/**
	 * When to show full health details.
	 */
	private ShowDetails showDetails = ShowDetails.NEVER;

	/**
	 * Roles used to determine whether or not a user is authorized to be shown details.
	 * When empty, all authenticated users are authorized.
	 */
	private Set<String> roles = new HashSet<>();

	public Status getStatus() {
		return this.status;
	}

	public ShowDetails getShowDetails() {
		return this.showDetails;
	}

	public void setShowDetails(ShowDetails showDetails) {
		this.showDetails = showDetails;
	}

	public Set<String> getRoles() {
		return this.roles;
	}

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
		private List<String> order = null;

		/**
		 * Mapping of health statuses to HTTP status codes. By default, registered health
		 * statuses map to sensible defaults (for example, UP maps to 200).
		 */
		private final Map<String, Integer> httpMapping = new HashMap<>();

		public List<String> getOrder() {
			return this.order;
		}

		public void setOrder(List<String> statusOrder) {
			if (statusOrder != null && !statusOrder.isEmpty()) {
				this.order = statusOrder;
			}
		}

		public Map<String, Integer> getHttpMapping() {
			return this.httpMapping;
		}

	}

	/**
	 * Options for showing details in responses from the {@link HealthEndpoint} web
	 * extensions.
	 */
	public enum ShowDetails {

		/**
		 * Never show details in the response.
		 */
		NEVER,

		/**
		 * Show details in the response when accessed by an authorized user.
		 */
		WHEN_AUTHORIZED,

		/**
		 * Always show details in the response.
		 */
		ALWAYS

	}

}
