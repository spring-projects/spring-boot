/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import java.time.Duration;

import io.micrometer.prometheus.HistogramFlavor;
import io.micrometer.prometheus.PrometheusConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link PrometheusProperties} to a {@link PrometheusConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class PrometheusPropertiesConfigAdapter extends PropertiesConfigAdapter<PrometheusProperties>
		implements PrometheusConfig {

	/**
	 * Constructs a new PrometheusPropertiesConfigAdapter with the specified
	 * PrometheusProperties.
	 * @param properties the PrometheusProperties to be used by the adapter
	 */
	PrometheusPropertiesConfigAdapter(PrometheusProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for Prometheus metrics export.
	 * @return the prefix for Prometheus metrics export
	 */
	@Override
	public String prefix() {
		return "management.prometheus.metrics.export";
	}

	/**
	 * Retrieves the value associated with the specified key from the
	 * PrometheusPropertiesConfigAdapter.
	 * @param key the key whose associated value is to be retrieved
	 * @return the value to which the specified key is mapped, or null if the key is not
	 * found
	 */
	@Override
	public String get(String key) {
		return null;
	}

	/**
	 * Returns the value of the descriptions property.
	 * @return the value of the descriptions property
	 */
	@Override
	public boolean descriptions() {
		return get(PrometheusProperties::isDescriptions, PrometheusConfig.super::descriptions);
	}

	/**
	 * Returns the histogram flavor for the Prometheus configuration.
	 * @return the histogram flavor
	 */
	@Override
	public HistogramFlavor histogramFlavor() {
		return get(PrometheusProperties::getHistogramFlavor, PrometheusConfig.super::histogramFlavor);
	}

	/**
	 * Returns the step duration for the Prometheus configuration.
	 * @return the step duration
	 */
	@Override
	public Duration step() {
		return get(PrometheusProperties::getStep, PrometheusConfig.super::step);
	}

}
