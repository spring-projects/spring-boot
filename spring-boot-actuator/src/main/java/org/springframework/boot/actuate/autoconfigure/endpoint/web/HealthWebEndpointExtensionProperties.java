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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.web.HealthWebEndpointExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the {@link HealthWebEndpointExtension}.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "endpoints.health")
public class HealthWebEndpointExtensionProperties {

	/**
	 * Mapping of health statuses to HttpStatus codes. By default, registered health
	 * statuses map to sensible defaults (i.e. UP maps to 200).
	 */
	private Map<String, Integer> mapping = new HashMap<>();

	public Map<String, Integer> getMapping() {
		return this.mapping;
	}

	public void setMapping(Map<String, Integer> mapping) {
		this.mapping = mapping;
	}

}
