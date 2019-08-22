/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Dynatrace
 * metrics export.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.dynatrace")
public class DynatraceProperties extends StepRegistryProperties {

	/**
	 * Dynatrace authentication token.
	 */
	private String apiToken;

	/**
	 * ID of the custom device that is exporting metrics to Dynatrace.
	 */
	private String deviceId;

	/**
	 * Technology type for exported metrics. Used to group metrics under a logical
	 * technology name in the Dynatrace UI.
	 */
	private String technologyType = "java";

	/**
	 * URI to ship metrics to. Should be used for SaaS, self managed instances or to
	 * en-route through an internal proxy.
	 */
	private String uri;

	public String getApiToken() {
		return this.apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getDeviceId() {
		return this.deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getTechnologyType() {
		return this.technologyType;
	}

	public void setTechnologyType(String technologyType) {
		this.technologyType = technologyType;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
