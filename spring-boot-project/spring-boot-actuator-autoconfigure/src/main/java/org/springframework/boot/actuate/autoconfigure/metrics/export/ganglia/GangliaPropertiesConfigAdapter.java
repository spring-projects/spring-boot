/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import info.ganglia.gmetric4j.gmetric.GMetric;
import io.micrometer.ganglia.GangliaConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GangliaProperties} to a {@link GangliaConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GangliaPropertiesConfigAdapter
		extends PropertiesConfigAdapter<GangliaProperties, GangliaConfig>
		implements GangliaConfig {

	private static final GangliaConfig DEFAULTS = (k) -> null;

	GangliaPropertiesConfigAdapter(GangliaProperties properties) {
		super(properties, DEFAULTS);
	}

	@Override
	public String get(String k) {
		return null;
	}

	@Override
	public boolean enabled() {
		return get(GangliaProperties::getEnabled, GangliaConfig::enabled);
	}

	@Override
	public Duration step() {
		return get(GangliaProperties::getStep, GangliaConfig::step);
	}

	@Override
	public TimeUnit rateUnits() {
		return get(GangliaProperties::getRateUnits, GangliaConfig::rateUnits);
	}

	@Override
	public TimeUnit durationUnits() {
		return get(GangliaProperties::getDurationUnits, GangliaConfig::durationUnits);
	}

	@Override
	public String protocolVersion() {
		return get(GangliaProperties::getProtocolVersion, GangliaConfig::protocolVersion);
	}

	@Override
	public GMetric.UDPAddressingMode addressingMode() {
		return get(GangliaProperties::getAddressingMode, GangliaConfig::addressingMode);
	}

	@Override
	public int ttl() {
		return get(GangliaProperties::getTimeToLive, GangliaConfig::ttl);
	}

	@Override
	public String host() {
		return get(GangliaProperties::getHost, GangliaConfig::host);
	}

	@Override
	public int port() {
		return get(GangliaProperties::getPort, GangliaConfig::port);
	}

}
