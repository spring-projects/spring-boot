/*
 * Copyright 2012-2015 the original author or authors.
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

	private static final String ENDPOINTS_SENSITIVE_PROPERTY = "endpoints.sensitive";

	/**
	 * Enable endpoints.
	 */
	private Boolean enabled = true;

	/**
	 * Default endpoint sensitive setting.
	 */
	private Boolean sensitive;

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getSensitive() {
		return this.sensitive;
	}

	public void setSensitive(Boolean sensitive) {
		this.sensitive = sensitive;
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

	/**
	 * Determine if an endpoint is sensitive based on its specific property and taking
	 * into account the global default.
	 * @param environment the Spring environment or {@code null}.
	 * @param sensitive the endpoint property or {@code null}
	 * @param sensitiveDefault the default setting to use if no environment property is
	 * defined
	 * @return if the endpoint is sensitive
	 */
	public static boolean isSensitive(Environment environment, Boolean sensitive,
			boolean sensitiveDefault) {
		if (sensitive != null) {
			return sensitive;
		}
		if (environment != null
				&& environment.containsProperty(ENDPOINTS_SENSITIVE_PROPERTY)) {
			return environment.getProperty(ENDPOINTS_SENSITIVE_PROPERTY, Boolean.class);
		}
		return sensitiveDefault;
	}

}
