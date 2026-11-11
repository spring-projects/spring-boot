/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.stackdriver;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Stackdriver
 * metrics export.
 *
 * @author Johannes Graf
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("management.stackdriver.metrics.export")
public class StackdriverProperties extends StepRegistryProperties {

	/**
	 * Identifier of the Google Cloud project to monitor.
	 */
	private @Nullable String projectId;

	/**
	 * Monitored resource type.
	 */
	private String resourceType = "global";

	/**
	 * Monitored resource's labels.
	 */
	private @Nullable Map<String, String> resourceLabels;

	/**
	 * Whether to use semantically correct metric types. When false, counter metrics are
	 * published as the GAUGE MetricKind. When true, counter metrics are published as the
	 * CUMULATIVE MetricKind.
	 */
	private boolean useSemanticMetricTypes;

	/**
	 * Prefix for metric type. Valid prefixes are described in the Google Cloud
	 * documentation (https://cloud.google.com/monitoring/custom-metrics#identifier).
	 */
	private String metricTypePrefix = "custom.googleapis.com/";

	/**
	 * Whether it should be attempted to create a metric descriptor before writing a time
	 * series.
	 */
	private boolean autoCreateMetricDescriptors = true;

	public @Nullable String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(@Nullable String projectId) {
		this.projectId = projectId;
	}

	public String getResourceType() {
		return this.resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public @Nullable Map<String, String> getResourceLabels() {
		return this.resourceLabels;
	}

	public void setResourceLabels(@Nullable Map<String, String> resourceLabels) {
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

	public boolean isAutoCreateMetricDescriptors() {
		return this.autoCreateMetricDescriptors;
	}

	public void setAutoCreateMetricDescriptors(boolean autoCreateMetricDescriptors) {
		this.autoCreateMetricDescriptors = autoCreateMetricDescriptors;
	}

}
