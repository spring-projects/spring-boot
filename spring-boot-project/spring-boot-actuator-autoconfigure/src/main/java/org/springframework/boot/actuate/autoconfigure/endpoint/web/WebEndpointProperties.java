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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for web management endpoints.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.endpoints.web")
public class WebEndpointProperties {

	private final Exposure exposure = new Exposure();

	/**
	 * Base path for Web endpoints. Relative to the servlet context path
	 * (server.servlet.context-path) or WebFlux base path (spring.webflux.base-path) when
	 * the management server is sharing the main server port. Relative to the management
	 * server base path (management.server.base-path) when a separate management server
	 * port (management.server.port) is configured.
	 */
	private String basePath = "/actuator";

	/**
	 * Mapping between endpoint IDs and the path that should expose them.
	 */
	private final Map<String, String> pathMapping = new LinkedHashMap<>();

	private final Discovery discovery = new Discovery();

	/**
	 * Returns the exposure of the WebEndpointProperties.
	 * @return the exposure of the WebEndpointProperties
	 */
	public Exposure getExposure() {
		return this.exposure;
	}

	/**
	 * Returns the base path of the web endpoint.
	 * @return the base path of the web endpoint
	 */
	public String getBasePath() {
		return this.basePath;
	}

	/**
	 * Sets the base path for the web endpoint.
	 * @param basePath the base path to be set
	 * @throws IllegalArgumentException if the base path does not start with '/' or is not
	 * empty
	 */
	public void setBasePath(String basePath) {
		Assert.isTrue(basePath.isEmpty() || basePath.startsWith("/"), "Base path must start with '/' or be empty");
		this.basePath = cleanBasePath(basePath);
	}

	/**
	 * Cleans the base path by removing the trailing slash if present.
	 * @param basePath the base path to be cleaned
	 * @return the cleaned base path
	 */
	private String cleanBasePath(String basePath) {
		if (StringUtils.hasText(basePath) && basePath.endsWith("/")) {
			return basePath.substring(0, basePath.length() - 1);
		}
		return basePath;
	}

	/**
	 * Returns the path mapping for the web endpoint.
	 * @return the path mapping as a Map of String keys and String values
	 */
	public Map<String, String> getPathMapping() {
		return this.pathMapping;
	}

	/**
	 * Returns the Discovery object associated with this WebEndpointProperties instance.
	 * @return the Discovery object
	 */
	public Discovery getDiscovery() {
		return this.discovery;
	}

	/**
	 * Exposure class.
	 */
	public static class Exposure {

		/**
		 * Endpoint IDs that should be included or '*' for all.
		 */
		private Set<String> include = new LinkedHashSet<>();

		/**
		 * Endpoint IDs that should be excluded or '*' for all.
		 */
		private Set<String> exclude = new LinkedHashSet<>();

		/**
		 * Returns the set of strings representing the include values.
		 * @return the set of strings representing the include values
		 */
		public Set<String> getInclude() {
			return this.include;
		}

		/**
		 * Sets the include set.
		 * @param include the set of strings to be included
		 */
		public void setInclude(Set<String> include) {
			this.include = include;
		}

		/**
		 * Returns the set of excluded strings.
		 * @return the set of excluded strings
		 */
		public Set<String> getExclude() {
			return this.exclude;
		}

		/**
		 * Sets the set of strings to exclude.
		 * @param exclude the set of strings to exclude
		 */
		public void setExclude(Set<String> exclude) {
			this.exclude = exclude;
		}

	}

	/**
	 * Discovery class.
	 */
	public static class Discovery {

		/**
		 * Whether the discovery page is enabled.
		 */
		private boolean enabled = true;

		/**
		 * Returns the current status of the enabled flag.
		 * @return true if the enabled flag is set to true, false otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the Discovery.
		 * @param enabled the enabled status to be set
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
