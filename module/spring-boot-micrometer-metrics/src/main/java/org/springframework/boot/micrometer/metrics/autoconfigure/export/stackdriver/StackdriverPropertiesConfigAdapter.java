/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.stackdriver;

import java.util.Map;

import io.micrometer.stackdriver.StackdriverConfig;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link StackdriverProperties} to a {@link StackdriverConfig}.
 *
 * @author Johannes Graf
 * @since 4.0.0
 */
public class StackdriverPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<StackdriverProperties>
		implements StackdriverConfig {

	public StackdriverPropertiesConfigAdapter(StackdriverProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.stackdriver.metrics.export";
	}

	@Override
	public String projectId() {
		return obtain(StackdriverProperties::getProjectId, StackdriverConfig.super::projectId);
	}

	@Override
	public String resourceType() {
		return obtain(StackdriverProperties::getResourceType, StackdriverConfig.super::resourceType);
	}

	@Override
	public Map<String, String> resourceLabels() {
		return obtain(StackdriverProperties::getResourceLabels, StackdriverConfig.super::resourceLabels);
	}

	@Override
	public boolean useSemanticMetricTypes() {
		return obtain(StackdriverProperties::isUseSemanticMetricTypes, StackdriverConfig.super::useSemanticMetricTypes);
	}

	@Override
	public String metricTypePrefix() {
		return obtain(StackdriverProperties::getMetricTypePrefix, StackdriverConfig.super::metricTypePrefix);
	}

	@Override
	public boolean autoCreateMetricDescriptors() {
		return obtain(StackdriverProperties::isAutoCreateMetricDescriptors,
				StackdriverConfig.super::autoCreateMetricDescriptors);
	}

}
