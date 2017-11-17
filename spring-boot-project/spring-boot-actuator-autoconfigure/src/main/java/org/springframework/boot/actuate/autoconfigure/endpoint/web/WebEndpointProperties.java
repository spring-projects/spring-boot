/*
 * Copyright 2012-2017 the original author or authors.
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

/**
 * Configuration properties for web management endpoints.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.endpoints.web")
public class WebEndpointProperties {

	/**
	 * Base path for Web endpoints. Relative to server.context-path or
	 * management.server.context-path if management.server.port is configured.
	 */
	private String basePath = "/application";

	/**
	 * Endpoint IDs that should be exposed or '*' for all.
	 */
	private Set<String> expose = new LinkedHashSet<>();

	/**
	 * Endpoint IDs that should be excluded.
	 */
	private Set<String> exclude = new LinkedHashSet<>();

	/**
	 * Mapping between endpoint IDs and the path that should expose them.
	 */
	private final Map<String, String> pathMapping = new LinkedHashMap<>();

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public Set<String> getExpose() {
		return this.expose;
	}

	public void setExpose(Set<String> expose) {
		this.expose = expose;
	}

	public Set<String> getExclude() {
		return this.exclude;
	}

	public void setExclude(Set<String> exclude) {
		this.exclude = exclude;
	}

	public Map<String, String> getPathMapping() {
		return this.pathMapping;
	}

}
