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

package org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import info.ganglia.gmetric4j.gmetric.GMetric;
import io.micrometer.ganglia.GangliaConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GangliaProperties} to a {@link GangliaConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GangliaPropertiesConfigAdapter extends PropertiesConfigAdapter<GangliaProperties> implements GangliaConfig {

	/**
     * Constructs a new GangliaPropertiesConfigAdapter with the specified GangliaProperties.
     * 
     * @param properties the GangliaProperties to be used for configuring the adapter
     */
    GangliaPropertiesConfigAdapter(GangliaProperties properties) {
		super(properties);
	}

	/**
     * Returns the prefix for Ganglia metrics export configuration.
     *
     * @return the prefix for Ganglia metrics export configuration
     */
    @Override
	public String prefix() {
		return "management.ganglia.metrics.export";
	}

	/**
     * Retrieves the value associated with the specified key from the GangliaPropertiesConfigAdapter.
     * 
     * @param k the key whose associated value is to be retrieved
     * @return the value to which the specified key is mapped, or null if the key is not found
     */
    @Override
	public String get(String k) {
		return null;
	}

	/**
     * Returns whether Ganglia is enabled.
     *
     * @return {@code true} if Ganglia is enabled, {@code false} otherwise.
     */
    @Override
	public boolean enabled() {
		return get(GangliaProperties::isEnabled, GangliaConfig.super::enabled);
	}

	/**
     * Returns the step duration for the Ganglia configuration.
     * This method retrieves the step duration from the GangliaProperties class using the getStep() method.
     * If the getStep() method returns null, the step duration is retrieved from the GangliaConfig interface using the default step() method.
     * 
     * @return the step duration for the Ganglia configuration
     */
    @Override
	public Duration step() {
		return get(GangliaProperties::getStep, GangliaConfig.super::step);
	}

	/**
     * Returns the duration units for the Ganglia configuration.
     * 
     * @return the duration units
     */
    @Override
	public TimeUnit durationUnits() {
		return get(GangliaProperties::getDurationUnits, GangliaConfig.super::durationUnits);
	}

	/**
     * Returns the addressing mode for the GMetric UDP connection.
     *
     * @return the addressing mode for the GMetric UDP connection
     */
    @Override
	public GMetric.UDPAddressingMode addressingMode() {
		return get(GangliaProperties::getAddressingMode, GangliaConfig.super::addressingMode);
	}

	/**
     * Returns the time to live value.
     * 
     * @return the time to live value
     */
    @Override
	public int ttl() {
		return get(GangliaProperties::getTimeToLive, GangliaConfig.super::ttl);
	}

	/**
     * Returns the host value from the GangliaPropertiesConfigAdapter class.
     * If the host value is not present in the GangliaPropertiesConfigAdapter class,
     * it falls back to the host value from the GangliaConfig interface.
     *
     * @return the host value from the GangliaPropertiesConfigAdapter class,
     *         or the host value from the GangliaConfig interface if not present
     */
    @Override
	public String host() {
		return get(GangliaProperties::getHost, GangliaConfig.super::host);
	}

	/**
     * Returns the port number for the Ganglia configuration.
     * 
     * @return the port number
     */
    @Override
	public int port() {
		return get(GangliaProperties::getPort, GangliaConfig.super::port);
	}

}
