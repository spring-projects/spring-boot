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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.ganglia;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import info.ganglia.gmetric4j.gmetric.GMetric;
import io.micrometer.ganglia.GangliaConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GangliaProperties} to a {@link GangliaConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GangliaPropertiesConfigAdapter extends PropertiesConfigAdapter<GangliaProperties> implements GangliaConfig {

	GangliaPropertiesConfigAdapter(GangliaProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.ganglia.metrics.export";
	}

	@Override
	public @Nullable String get(String k) {
		return null;
	}

	@Override
	public boolean enabled() {
		return getRequired(GangliaProperties::isEnabled, GangliaConfig.super::enabled);
	}

	@Override
	public Duration step() {
		return getRequired(GangliaProperties::getStep, GangliaConfig.super::step);
	}

	@Override
	public TimeUnit durationUnits() {
		return getRequired(GangliaProperties::getDurationUnits, GangliaConfig.super::durationUnits);
	}

	@Override
	public GMetric.UDPAddressingMode addressingMode() {
		return getRequired(GangliaProperties::getAddressingMode, GangliaConfig.super::addressingMode);
	}

	@Override
	public int ttl() {
		return getRequired(GangliaProperties::getTimeToLive, GangliaConfig.super::ttl);
	}

	@Override
	public String host() {
		return getRequired(GangliaProperties::getHost, GangliaConfig.super::host);
	}

	@Override
	public int port() {
		return getRequired(GangliaProperties::getPort, GangliaConfig.super::port);
	}

}
