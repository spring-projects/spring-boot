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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * Configuration properties for some health properties.
 *
 * @author Christian Dupuis
 * @since 2.0.0
 * @deprecated since 2.2.0 in favor of {@link HealthEndpointProperties}
 */
@Deprecated
@ConfigurationProperties(prefix = "management.health.status")
public class HealthIndicatorProperties {

	private List<String> order = new ArrayList<>();

	private final Map<String, Integer> httpMapping = new LinkedHashMap<>();

	@DeprecatedConfigurationProperty(replacement = "management.endpoint.health.status.order")
	public List<String> getOrder() {
		return this.order;
	}

	public void setOrder(List<String> order) {
		this.order = order;
	}

	@DeprecatedConfigurationProperty(replacement = "management.endpoint.health.status.http-mapping")
	public Map<String, Integer> getHttpMapping() {
		return this.httpMapping;
	}

}
