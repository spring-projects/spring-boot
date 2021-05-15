/*
 * Copyright 2012-2020 the original author or authors.
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

	public StackdriverPropertiesConfigAdapter(StackdriverProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.metrics.export.stackdriver";
	}

	@Override
	public String projectId() {
		return get(StackdriverProperties::getProjectId, StackdriverConfig.super::projectId);
	}

	@Override
	public String resourceType() {
		return get(StackdriverProperties::getResourceType, StackdriverConfig.super::resourceType);
	}

}
