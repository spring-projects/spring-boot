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

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getResourceType() {
		return this.resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public Map<String, String> getResourceLabels() {
		return this.resourceLabels;
	}

	public void setResourceLabels(Map<String, String> resourceLabels) {
		this.resourceLabels = resourceLabels;
	}

	public boolean isUseSemanticMetricTypes() {
		return this.useSemanticMetricTypes;
	}

	public void setUseSemanticMetricTypes(boolean useSemanticMetricTypes) {
		this.useSemanticMetricTypes = useSemanticMetricTypes;
	}

	public String getMetricTypePrefix() {
		return this.metricTypePrefix;
	}

	public void setMetricTypePrefix(String metricTypePrefix) {
		this.metricTypePrefix = metricTypePrefix;
	}

}
