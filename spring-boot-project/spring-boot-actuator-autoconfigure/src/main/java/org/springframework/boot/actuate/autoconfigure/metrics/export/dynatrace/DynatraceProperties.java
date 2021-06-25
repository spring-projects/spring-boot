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

import java.util.Map;

import io.micrometer.dynatrace.DynatraceApiVersion;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Dynatrace
 * metrics export.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.dynatrace")
public class DynatraceProperties extends StepRegistryProperties {

	/**
	 * The Dynatrace metrics API version that metrics should be sent to. Defaults to v1.
	 * Required to define which API is used for export.
	 */
	private DynatraceApiVersion apiVersion = DynatraceApiVersion.V1;

	/**
	 * Dynatrace authentication token.
	 *
	 * API v1: required, API v2: optional
	 */
	private String apiToken;

	/**
	 * ID of the custom device that is exporting metrics to Dynatrace.
	 *
	 * API v1: required, API v2: not applicable (ignored)
	 */
	private String deviceId;

	/**
	 * Technology type for exported metrics. Used to group metrics under a logical
	 * technology name in the Dynatrace UI.
	 *
	 * API v1: required, API v2: not applicable (ignored)
	 */
	private String technologyType = "java";

	/**
	 * URI to ship metrics to. Should be used for SaaS, self managed instances or to
	 * en-route through an internal proxy.
	 *
	 * API v1: required, API v2: optional
	 */
	private String uri;

	/**
	 * Group for exported metrics. Used to specify custom device group name in the
	 * Dynatrace UI.
	 *
	 * API v1: required, API v2: not applicable (ignored)
	 */
	private String group;

	/**
	 * An optional prefix string that is added to all metrics exported.
	 *
	 * API v1: not applicable (ignored), API v2: optional
	 */
	private String metricKeyPrefix;

	/**
	 * An optional Boolean that allows enabling of the OneAgent metadata export. On by
	 * default.
	 *
	 * API v1: not applicable (ignored), API v2: optional
	 */
	private Boolean enrichWithOneAgentMetadata = true;

	/**
	 * Optional default dimensions that are added to all metrics in the form of key-value
	 * pairs. These are overwritten by Micrometer tags if they use the same key.
	 *
	 * API v1: not applicable (ignored), API v2: optional
	 */
	private Map<String, String> defaultDimensions;

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

	public String getGroup() {
		return this.group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getMetricKeyPrefix() {
		return this.metricKeyPrefix;
	}

	public void setMetricKeyPrefix(String metricKeyPrefix) {
		this.metricKeyPrefix = metricKeyPrefix;
	}

	public Boolean getEnrichWithOneAgentMetadata() {
		return this.enrichWithOneAgentMetadata;
	}

	public void setEnrichWithOneAgentMetadata(Boolean enrichWithOneAgentMetadata) {
		this.enrichWithOneAgentMetadata = enrichWithOneAgentMetadata;
	}

	public Map<String, String> getDefaultDimensions() {
		return this.defaultDimensions;
	}

	public void setDefaultDimensions(Map<String, String> defaultDimensions) {
		this.defaultDimensions = defaultDimensions;
	}

	public DynatraceApiVersion getApiVersion() {
		return this.apiVersion;
	}

	public void setApiVersion(DynatraceApiVersion apiVersion) {
		this.apiVersion = apiVersion;
	}

}
