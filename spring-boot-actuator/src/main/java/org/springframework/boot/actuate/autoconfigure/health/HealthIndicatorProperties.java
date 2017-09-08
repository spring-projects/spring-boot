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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for some health properties.
 *
 * @author Christian Dupuis
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.health.status")
public class HealthIndicatorProperties {

	/**
	 * Comma-separated list of health statuses in order of severity.
	 */
	private List<String> order = null;

	/**
	 * Mapping of health statuses to HttpStatus codes. By default, registered health
	 * statuses map to sensible defaults (i.e. UP maps to 200).
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
