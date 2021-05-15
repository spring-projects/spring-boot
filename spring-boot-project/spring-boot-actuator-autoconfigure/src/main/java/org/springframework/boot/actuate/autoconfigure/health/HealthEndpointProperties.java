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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @author Leo Li
 * @since 2.0.0
 */
@ConfigurationProperties("management.endpoint.health")
public class HealthEndpointProperties extends HealthProperties {

	/**
	 * When to show full health details.
	 */
	private Show showDetails = Show.NEVER;

	/**
	 * Health endpoint groups.
	 */
	private Map<String, Group> group = new LinkedHashMap<>();

	@Override
	public Show getShowDetails() {
		return this.showDetails;
	}

	public void setShowDetails(Show showDetails) {
		this.showDetails = showDetails;
	}

	public Map<String, Group> getGroup() {
		return this.group;
	}

	/**
	 * A health endpoint group.
	 */
	public static class Group extends HealthProperties {

		/**
		 * Health indicator IDs that should be included or '*' for all.
		 */
		private Set<String> include;

		/**
		 * Health indicator IDs that should be excluded or '*' for all.
		 */
		private Set<String> exclude;

		/**
		 * When to show full health details. Defaults to the value of
		 * 'management.endpoint.health.show-details'.
		 */
		private Show showDetails;

		public Set<String> getInclude() {
			return this.include;
		}

		public void setInclude(Set<String> include) {
			this.include = include;
		}

		public Set<String> getExclude() {
			return this.exclude;
		}

		public void setExclude(Set<String> exclude) {
			this.exclude = exclude;
		}

		@Override
		public Show getShowDetails() {
			return this.showDetails;
		}

		public void setShowDetails(Show showDetails) {
			this.showDetails = showDetails;
		}

	}

}
