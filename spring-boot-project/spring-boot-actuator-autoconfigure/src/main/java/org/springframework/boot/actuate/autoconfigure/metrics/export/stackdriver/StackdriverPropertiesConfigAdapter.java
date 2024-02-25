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

import io.micrometer.stackdriver.StackdriverConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link StackdriverProperties} to a {@link StackdriverConfig}.
 *
 * @author Johannes Graf
 * @since 2.3.0
 */
public class StackdriverPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<StackdriverProperties>
		implements StackdriverConfig {

	/**
	 * Constructs a new StackdriverPropertiesConfigAdapter with the specified
	 * StackdriverProperties.
	 * @param properties the StackdriverProperties to be used for configuring the adapter
	 */
	public StackdriverPropertiesConfigAdapter(StackdriverProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for Stackdriver metrics export configuration.
	 * @return the prefix for Stackdriver metrics export configuration
	 */
	@Override
	public String prefix() {
		return "management.stackdriver.metrics.export";
	}

	/**
	 * Returns the project ID.
	 * @return the project ID
	 */
	@Override
	public String projectId() {
		return get(StackdriverProperties::getProjectId, StackdriverConfig.super::projectId);
	}

	/**
	 * Returns the resource type for Stackdriver.
	 * @return the resource type
	 */
	@Override
	public String resourceType() {
		return get(StackdriverProperties::getResourceType, StackdriverConfig.super::resourceType);
	}

	/**
	 * Returns the resource labels for Stackdriver.
	 * @return a map containing the resource labels
	 */
	@Override
	public Map<String, String> resourceLabels() {
		return get(StackdriverProperties::getResourceLabels, StackdriverConfig.super::resourceLabels);
	}

	/**
	 * Returns a boolean value indicating whether to use semantic metric types.
	 * @return true if semantic metric types should be used, false otherwise
	 */
	@Override
	public boolean useSemanticMetricTypes() {
		return get(StackdriverProperties::isUseSemanticMetricTypes, StackdriverConfig.super::useSemanticMetricTypes);
	}

	/**
	 * Returns the metric type prefix.
	 * @return the metric type prefix
	 */
	@Override
	public String metricTypePrefix() {
		return get(StackdriverProperties::getMetricTypePrefix, StackdriverConfig.super::metricTypePrefix);
	}

}
