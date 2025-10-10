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
