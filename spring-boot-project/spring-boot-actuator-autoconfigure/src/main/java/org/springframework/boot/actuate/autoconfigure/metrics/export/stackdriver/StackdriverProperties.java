/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.stackdriver;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Stackdriver
 * metrics export.
 *
 * @author Johannes Graf
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@ConfigurationProperties(prefix = "management.stackdriver.metrics.export")
public class StackdriverProperties extends StepRegistryProperties {

	/**
	 * Identifier of the Google Cloud project to monitor.
	 */
	private String projectId;

	/**
	 * Monitored resource type.
	 */
	private String resourceType = "global";

	/**
	 * Monitored resource's labels.
	 */
	private Map<String, String> resourceLabels;

	/**
	 * Whether to use semantically correct metric types. When false, counter metrics are
	 * published as the GAUGE MetricKind. When true, counter metrics are published as the
	 * CUMULATIVE MetricKind.
	 */
	private boolean useSemanticMetricTypes = false;

	/**
	 * Prefix for metric type. Valid prefixes are described in the Google Cloud
	 * documentation (https://cloud.google.com/monitoring/custom-metrics#identifier).
	 */
	private String metricTypePrefix = "custom.googleapis.com/";

	/**
	 * Returns the project ID.
	 * @return the project ID
	 */
	public String getProjectId() {
		return this.projectId;
	}

	/**
	 * Sets the project ID for Stackdriver.
	 * @param projectId the project ID to be set
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * Returns the resource type of the StackdriverProperties object.
	 * @return the resource type of the StackdriverProperties object
	 */
	public String getResourceType() {
		return this.resourceType;
	}

	/**
	 * Sets the resource type for Stackdriver.
	 * @param resourceType the resource type to be set
	 */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * Returns the resource labels associated with the Stackdriver properties.
	 * @return a Map containing the resource labels as key-value pairs
	 */
	public Map<String, String> getResourceLabels() {
		return this.resourceLabels;
	}

	/**
	 * Sets the resource labels for the StackdriverProperties.
	 * @param resourceLabels the resource labels to be set
	 */
	public void setResourceLabels(Map<String, String> resourceLabels) {
		this.resourceLabels = resourceLabels;
	}

	/**
	 * Returns the value of the flag indicating whether to use semantic metric types.
	 * @return true if semantic metric types are to be used, false otherwise
	 */
	public boolean isUseSemanticMetricTypes() {
		return this.useSemanticMetricTypes;
	}

	/**
	 * Sets whether to use semantic metric types.
	 * @param useSemanticMetricTypes true to use semantic metric types, false otherwise
	 */
	public void setUseSemanticMetricTypes(boolean useSemanticMetricTypes) {
		this.useSemanticMetricTypes = useSemanticMetricTypes;
	}

	/**
	 * Returns the metric type prefix.
	 * @return the metric type prefix
	 */
	public String getMetricTypePrefix() {
		return this.metricTypePrefix;
	}

	/**
	 * Sets the metric type prefix for Stackdriver.
	 * @param metricTypePrefix the metric type prefix to be set
	 */
	public void setMetricTypePrefix(String metricTypePrefix) {
		this.metricTypePrefix = metricTypePrefix;
	}

}
