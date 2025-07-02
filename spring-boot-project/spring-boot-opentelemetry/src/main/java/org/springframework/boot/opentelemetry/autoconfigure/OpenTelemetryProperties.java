/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry")
public class OpenTelemetryProperties {

	/**
	 * Whether to reuse registered global OpenTelemetry.
	 */
	private boolean reuseRegisteredGlobal;

	/**
	 * Resource attributes.
	 */
	private Map<String, String> resourceAttributes = new HashMap<>();

	public boolean isReuseRegisteredGlobal() {
		return this.reuseRegisteredGlobal;
	}

	public void setReuseRegisteredGlobal(boolean reuseRegisteredGlobal) {
		this.reuseRegisteredGlobal = reuseRegisteredGlobal;
	}

	public Map<String, String> getResourceAttributes() {
		return this.resourceAttributes;
	}

	public void setResourceAttributes(Map<String, String> resourceAttributes) {
		this.resourceAttributes = resourceAttributes;
	}

}
