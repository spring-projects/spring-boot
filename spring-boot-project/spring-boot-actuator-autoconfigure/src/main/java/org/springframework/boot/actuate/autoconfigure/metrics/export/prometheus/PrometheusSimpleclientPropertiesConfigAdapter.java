/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link PrometheusSimpleclientProperties} to a
 * {@link io.micrometer.prometheus.PrometheusConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
@SuppressWarnings({ "deprecation", "removal" })
class PrometheusSimpleclientPropertiesConfigAdapter extends PropertiesConfigAdapter<PrometheusSimpleclientProperties>
		implements io.micrometer.prometheus.PrometheusConfig {

	PrometheusSimpleclientPropertiesConfigAdapter(PrometheusSimpleclientProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.prometheus.metrics.export";
	}

	@Override
	public String get(String key) {
		return null;
	}

	@Override
	public boolean descriptions() {
		return get(PrometheusSimpleclientProperties::isDescriptions,
				io.micrometer.prometheus.PrometheusConfig.super::descriptions);
	}

	@Override
	public io.micrometer.prometheus.HistogramFlavor histogramFlavor() {
		return get(PrometheusSimpleclientProperties::getHistogramFlavor,
				io.micrometer.prometheus.PrometheusConfig.super::histogramFlavor);
	}

	@Override
	public Duration step() {
		return get(PrometheusSimpleclientProperties::getStep, io.micrometer.prometheus.PrometheusConfig.super::step);
	}

}
