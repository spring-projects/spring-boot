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

package org.springframework.boot.actuate.autoconfigure.metrics.export.graphite;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteProtocol;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GraphiteProperties} to a {@link GraphiteConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GraphitePropertiesConfigAdapter extends PropertiesConfigAdapter<GraphiteProperties> implements GraphiteConfig {

	/**
	 * Constructs a new GraphitePropertiesConfigAdapter with the specified
	 * GraphiteProperties.
	 * @param properties the GraphiteProperties to be used for configuring the adapter
	 */
	GraphitePropertiesConfigAdapter(GraphiteProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for the Graphite metrics export configuration. The prefix is
	 * used to identify the properties related to Graphite metrics export in the
	 * configuration file.
	 * @return the prefix for the Graphite metrics export configuration
	 */
	@Override
	public String prefix() {
		return "management.graphite.metrics.export";
	}

	/**
	 * Retrieves the value associated with the specified key from the
	 * GraphitePropertiesConfigAdapter.
	 * @param k the key whose associated value is to be retrieved
	 * @return the value to which the specified key is mapped, or null if the key is not
	 * found
	 */
	@Override
	public String get(String k) {
		return null;
	}

	/**
	 * Returns whether the Graphite properties are enabled.
	 * @return {@code true} if the Graphite properties are enabled, {@code false}
	 * otherwise.
	 */
	@Override
	public boolean enabled() {
		return get(GraphiteProperties::isEnabled, GraphiteConfig.super::enabled);
	}

	/**
	 * Returns the step duration.
	 * @return the step duration
	 */
	@Override
	public Duration step() {
		return get(GraphiteProperties::getStep, GraphiteConfig.super::step);
	}

	/**
	 * Returns the rate units for the Graphite properties.
	 * @return the rate units for the Graphite properties
	 */
	@Override
	public TimeUnit rateUnits() {
		return get(GraphiteProperties::getRateUnits, GraphiteConfig.super::rateUnits);
	}

	/**
	 * Returns the duration units for the Graphite properties.
	 * @return the duration units for the Graphite properties
	 */
	@Override
	public TimeUnit durationUnits() {
		return get(GraphiteProperties::getDurationUnits, GraphiteConfig.super::durationUnits);
	}

	/**
	 * Returns the host value for the GraphitePropertiesConfigAdapter. If the host value
	 * is not present in the GraphiteProperties, it falls back to the default host value
	 * from GraphiteConfig.
	 * @return the host value for the GraphitePropertiesConfigAdapter
	 */
	@Override
	public String host() {
		return get(GraphiteProperties::getHost, GraphiteConfig.super::host);
	}

	/**
	 * Returns the port number for the Graphite server.
	 * @return the port number
	 */
	@Override
	public int port() {
		return get(GraphiteProperties::getPort, GraphiteConfig.super::port);
	}

	/**
	 * Returns the protocol used for communication with the Graphite server.
	 * @return the protocol used for communication with the Graphite server
	 */
	@Override
	public GraphiteProtocol protocol() {
		return get(GraphiteProperties::getProtocol, GraphiteConfig.super::protocol);
	}

	/**
	 * Returns whether the graphite tags are enabled.
	 * @return {@code true} if the graphite tags are enabled, {@code false} otherwise.
	 */
	@Override
	public boolean graphiteTagsEnabled() {
		return get(GraphiteProperties::getGraphiteTagsEnabled, GraphiteConfig.super::graphiteTagsEnabled);
	}

	/**
	 * Returns an array of tags as prefix.
	 * @return the array of tags as prefix
	 */
	@Override
	public String[] tagsAsPrefix() {
		return get(GraphiteProperties::getTagsAsPrefix, GraphiteConfig.super::tagsAsPrefix);
	}

}
