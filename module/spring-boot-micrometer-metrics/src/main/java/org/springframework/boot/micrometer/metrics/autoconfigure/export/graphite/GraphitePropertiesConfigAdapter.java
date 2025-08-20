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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.graphite;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteProtocol;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GraphiteProperties} to a {@link GraphiteConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GraphitePropertiesConfigAdapter extends PropertiesConfigAdapter<GraphiteProperties> implements GraphiteConfig {

	GraphitePropertiesConfigAdapter(GraphiteProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.graphite.metrics.export";
	}

	@Override
	public @Nullable String get(String k) {
		return null;
	}

	@Override
	public boolean enabled() {
		return getRequired(GraphiteProperties::isEnabled, GraphiteConfig.super::enabled);
	}

	@Override
	public Duration step() {
		return getRequired(GraphiteProperties::getStep, GraphiteConfig.super::step);
	}

	@Override
	public TimeUnit rateUnits() {
		return getRequired(GraphiteProperties::getRateUnits, GraphiteConfig.super::rateUnits);
	}

	@Override
	public TimeUnit durationUnits() {
		return getRequired(GraphiteProperties::getDurationUnits, GraphiteConfig.super::durationUnits);
	}

	@Override
	public String host() {
		return getRequired(GraphiteProperties::getHost, GraphiteConfig.super::host);
	}

	@Override
	public int port() {
		return getRequired(GraphiteProperties::getPort, GraphiteConfig.super::port);
	}

	@Override
	public GraphiteProtocol protocol() {
		return getRequired(GraphiteProperties::getProtocol, GraphiteConfig.super::protocol);
	}

	@Override
	public boolean graphiteTagsEnabled() {
		return getRequired(GraphiteProperties::getGraphiteTagsEnabled, GraphiteConfig.super::graphiteTagsEnabled);
	}

	@Override
	public String[] tagsAsPrefix() {
		return getRequired(GraphiteProperties::getTagsAsPrefix, GraphiteConfig.super::tagsAsPrefix);
	}

}
