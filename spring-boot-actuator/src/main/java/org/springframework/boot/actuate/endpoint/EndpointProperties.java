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

package org.springframework.boot.actuate.endpoint;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * Global endpoint properties.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints")
public class EndpointProperties {

	private static final String ENDPOINTS_ENABLED_PROPERTY = "endpoints.enabled";

	/**
	 * Enable endpoints.
	 */
	private Boolean enabled = true;

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Determine if an endpoint is enabled based on its specific property and taking into
	 * account the global default.
	 * @param environment the Spring environment or {@code null}.
	 * @param enabled the endpoint property or {@code null}
	 * @return if the endpoint is enabled
	 */
	public static boolean isEnabled(Environment environment, Boolean enabled) {
		if (enabled != null) {
			return enabled;
		}
		if (environment != null
				&& environment.containsProperty(ENDPOINTS_ENABLED_PROPERTY)) {
			return environment.getProperty(ENDPOINTS_ENABLED_PROPERTY, Boolean.class);
		}
		return true;
	}

}
