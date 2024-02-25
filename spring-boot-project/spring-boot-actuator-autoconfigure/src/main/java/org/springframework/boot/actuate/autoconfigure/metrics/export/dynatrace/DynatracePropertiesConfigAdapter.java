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

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import java.util.Map;
import java.util.function.Function;

import io.micrometer.dynatrace.DynatraceApiVersion;
import io.micrometer.dynatrace.DynatraceConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace.DynatraceProperties.V1;
import org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace.DynatraceProperties.V2;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link DynatraceProperties} to a {@link DynatraceConfig}.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 */
class DynatracePropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<DynatraceProperties>
		implements DynatraceConfig {

	/**
	 * Constructs a new DynatracePropertiesConfigAdapter with the specified
	 * DynatraceProperties.
	 * @param properties the DynatraceProperties to be used for configuring the adapter
	 */
	DynatracePropertiesConfigAdapter(DynatraceProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for Dynatrace metrics export configuration properties.
	 * @return the prefix for Dynatrace metrics export configuration properties
	 */
	@Override
	public String prefix() {
		return "management.dynatrace.metrics.export";
	}

	/**
	 * Returns the API token for accessing the Dynatrace API.
	 * @return the API token
	 */
	@Override
	public String apiToken() {
		return get(DynatraceProperties::getApiToken, DynatraceConfig.super::apiToken);
	}

	/**
	 * Returns the device ID.
	 * @return the device ID
	 */
	@Override
	public String deviceId() {
		return get(v1(V1::getDeviceId), DynatraceConfig.super::deviceId);
	}

	/**
	 * Returns the technology type of the Dynatrace configuration.
	 * @return the technology type
	 */
	@Override
	public String technologyType() {
		return get(v1(V1::getTechnologyType), DynatraceConfig.super::technologyType);
	}

	/**
	 * Returns the URI value from DynatracePropertiesConfigAdapter. If the value is not
	 * present, it falls back to the default URI value from DynatraceConfig.
	 * @return the URI value
	 */
	@Override
	public String uri() {
		return get(DynatraceProperties::getUri, DynatraceConfig.super::uri);
	}

	/**
	 * Returns the group value from the DynatracePropertiesConfigAdapter class.
	 * @return the group value
	 */
	@Override
	public String group() {
		return get(v1(V1::getGroup), DynatraceConfig.super::group);
	}

	/**
	 * Returns the API version based on the properties configuration.
	 * @return the API version
	 */
	@Override
	public DynatraceApiVersion apiVersion() {
		return get((properties) -> (properties.getV1().getDeviceId() != null) ? DynatraceApiVersion.V1
				: DynatraceApiVersion.V2, DynatraceConfig.super::apiVersion);
	}

	/**
	 * Returns the metric key prefix for the Dynatrace configuration.
	 * @return the metric key prefix
	 */
	@Override
	public String metricKeyPrefix() {
		return get(v2(V2::getMetricKeyPrefix), DynatraceConfig.super::metricKeyPrefix);
	}

	/**
	 * Returns the default dimensions for the Dynatrace configuration.
	 * @return a map containing the default dimensions, where the key is the dimension
	 * name and the value is the dimension value
	 */
	@Override
	public Map<String, String> defaultDimensions() {
		return get(v2(V2::getDefaultDimensions), DynatraceConfig.super::defaultDimensions);
	}

	/**
	 * Enriches the configuration with Dynatrace metadata.
	 * @return true if the configuration is enriched with Dynatrace metadata, false
	 * otherwise
	 */
	@Override
	public boolean enrichWithDynatraceMetadata() {
		return get(v2(V2::isEnrichWithDynatraceMetadata), DynatraceConfig.super::enrichWithDynatraceMetadata);
	}

	/**
	 * Returns a boolean value indicating whether to use Dynatrace summary instruments.
	 * @return true if Dynatrace summary instruments should be used, false otherwise
	 */
	@Override
	public boolean useDynatraceSummaryInstruments() {
		return get(v2(V2::isUseDynatraceSummaryInstruments), DynatraceConfig.super::useDynatraceSummaryInstruments);
	}

	/**
	 * Export the meter metadata.
	 * @return true if the meter metadata should be exported, false otherwise
	 */
	@Override
	public boolean exportMeterMetadata() {
		return get(v2(V2::isExportMeterMetadata), DynatraceConfig.super::exportMeterMetadata);
	}

	/**
	 * Returns a function that extracts the value of V1 from a DynatraceProperties object
	 * using the provided getter function.
	 * @param getter the function used to extract the value of V1 from a
	 * DynatraceProperties object
	 * @param <V> the type of the value extracted from V1
	 * @return a function that extracts the value of V1 from a DynatraceProperties object
	 */
	private <V> Function<DynatraceProperties, V> v1(Function<V1, V> getter) {
		return (properties) -> getter.apply(properties.getV1());
	}

	/**
	 * Returns a function that extracts a value of type V from a DynatraceProperties
	 * object by applying the provided getter function to the V2 property of the
	 * DynatraceProperties object.
	 * @param getter the function that extracts a value of type V from a V2 object
	 * @return a function that extracts a value of type V from a DynatraceProperties
	 * object
	 * @param <V> the type of the value to be extracted
	 */
	private <V> Function<DynatraceProperties, V> v2(Function<V2, V> getter) {
		return (properties) -> getter.apply(properties.getV2());
	}

}
