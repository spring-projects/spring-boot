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

package org.springframework.boot.actuate.autoconfigure.metrics.export.graphite;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.graphite.GraphiteConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GraphiteProperties} to a {@link GraphiteConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GraphitePropertiesConfigAdapter
		extends PropertiesConfigAdapter<GraphiteProperties, GraphiteConfig>
		implements GraphiteConfig {

	private static final GraphiteConfig DEFAULTS = (k) -> null;

	GraphitePropertiesConfigAdapter(GraphiteProperties properties) {
		super(properties, DEFAULTS);
	}

	@Override
	public String get(String k) {
		return null;
	}

	@Override
	public boolean enabled() {
		return get(GraphiteProperties::getEnabled, GraphiteConfig::enabled);
	}

	@Override
	public Duration step() {
		return get(GraphiteProperties::getStep, GraphiteConfig::step);
	}

	@Override
	public TimeUnit rateUnits() {
		return get(GraphiteProperties::getRateUnits, GraphiteConfig::rateUnits);
	}

	@Override
	public TimeUnit durationUnits() {
		return get(GraphiteProperties::getDurationUnits, GraphiteConfig::durationUnits);
	}

	@Override
	public String host() {
		return get(GraphiteProperties::getHost, GraphiteConfig::host);
	}

	@Override
	public int port() {
		return get(GraphiteProperties::getPort, GraphiteConfig::port);
	}

}
