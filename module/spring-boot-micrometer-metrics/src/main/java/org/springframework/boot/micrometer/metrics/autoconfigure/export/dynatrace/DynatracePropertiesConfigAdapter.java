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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.dynatrace;

import java.util.Map;

import io.micrometer.dynatrace.DynatraceApiVersion;
import io.micrometer.dynatrace.DynatraceConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.dynatrace.DynatraceProperties.V1;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.dynatrace.DynatraceProperties.V2;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link DynatraceProperties} to a {@link DynatraceConfig}.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 */
class DynatracePropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<DynatraceProperties>
		implements DynatraceConfig {

	DynatracePropertiesConfigAdapter(DynatraceProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.dynatrace.metrics.export";
	}

	@Override
	public String apiToken() {
		return getRequired(DynatraceProperties::getApiToken, DynatraceConfig.super::apiToken);
	}

	@Override
	public String deviceId() {
		return getRequired(v1(V1::getDeviceId), DynatraceConfig.super::deviceId);
	}

	@Override
	public String technologyType() {
		return getRequired(v1(V1::getTechnologyType), DynatraceConfig.super::technologyType);
	}

	@Override
	public String uri() {
		return getRequired(DynatraceProperties::getUri, DynatraceConfig.super::uri);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String group() {
		return get(v1(V1::getGroup), DynatraceConfig.super::group);
	}

	@Override
	public DynatraceApiVersion apiVersion() {
		return getRequired((properties) -> (properties.getV1().getDeviceId() != null) ? DynatraceApiVersion.V1
				: DynatraceApiVersion.V2, DynatraceConfig.super::apiVersion);
	}

	@Override
	public String metricKeyPrefix() {
		return getRequired(v2(V2::getMetricKeyPrefix), DynatraceConfig.super::metricKeyPrefix);
	}

	@Override
	public Map<String, String> defaultDimensions() {
		return getRequired(v2(V2::getDefaultDimensions), DynatraceConfig.super::defaultDimensions);
	}

	@Override
	public boolean enrichWithDynatraceMetadata() {
		return getRequired(v2(V2::isEnrichWithDynatraceMetadata), DynatraceConfig.super::enrichWithDynatraceMetadata);
	}

	@Override
	public boolean useDynatraceSummaryInstruments() {
		return getRequired(v2(V2::isUseDynatraceSummaryInstruments),
				DynatraceConfig.super::useDynatraceSummaryInstruments);
	}

	@Override
	public boolean exportMeterMetadata() {
		return getRequired(v2(V2::isExportMeterMetadata), DynatraceConfig.super::exportMeterMetadata);
	}

	private <V> Getter<DynatraceProperties, V> v1(Getter<V1, V> getter) {
		return (properties) -> getter.get(properties.getV1());
	}

	private <V> Getter<DynatraceProperties, V> v2(Getter<V2, V> getter) {
		return (properties) -> getter.get(properties.getV2());
	}

}
