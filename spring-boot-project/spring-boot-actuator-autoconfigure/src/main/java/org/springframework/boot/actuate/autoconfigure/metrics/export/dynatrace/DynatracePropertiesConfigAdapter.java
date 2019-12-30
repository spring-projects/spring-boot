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

import io.micrometer.dynatrace.DynatraceConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link DynatraceProperties} to a {@link DynatraceConfig}.
 *
 * @author Andy Wilkinson
 */
class DynatracePropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<DynatraceProperties>
		implements DynatraceConfig {

	DynatracePropertiesConfigAdapter(DynatraceProperties properties) {
		super(properties);
	}

	@Override
	public String apiToken() {
		return get(DynatraceProperties::getApiToken, DynatraceConfig.super::apiToken);
	}

	@Override
	public String deviceId() {
		return get(DynatraceProperties::getDeviceId, DynatraceConfig.super::deviceId);
	}

	@Override
	public String technologyType() {
		return get(DynatraceProperties::getTechnologyType, DynatraceConfig.super::technologyType);
	}

	@Override
	public String uri() {
		return get(DynatraceProperties::getUri, DynatraceConfig.super::uri);
	}

	@Override
	public String group() {
		return get(DynatraceProperties::getGroup, DynatraceConfig.super::group);
	}

}
