/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
	 * Base path for Web endpoints. Relative to server.servlet.context-path or
	 * management.server.servlet.context-path if management.server.port is configured.
	 */
	private String basePath = "/actuator";

	/**
	 * Mapping between endpoint IDs and the path that should expose them.
	 */
	private final Map<String, String> pathMapping = new LinkedHashMap<>();

	public Exposure getExposure() {
		return this.exposure;
	}

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		Assert.isTrue(basePath.isEmpty() || basePath.startsWith("/"),
				"Base path must start with '/' or be empty");
		this.basePath = cleanBasePath(basePath);
	}

	private String cleanBasePath(String basePath) {
		if (StringUtils.hasText(basePath) && basePath.endsWith("/")) {
			return basePath.substring(0, basePath.length() - 1);
		}
		return basePath;
	}

	public Map<String, String> getPathMapping() {
		return this.pathMapping;
	}

	public static class Exposure {

		/**
		 * Endpoint IDs that should be included or '*' for all.
		 */
		private Set<String> include = new LinkedHashSet<>();

		/**
		 * Endpoint IDs that should be excluded or '*' for all.
		 */
		private Set<String> exclude = new LinkedHashSet<>();

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

	}

}
