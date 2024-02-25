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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import java.time.Duration;

import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdProtocol;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link StatsdProperties} to a {@link StatsdConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class StatsdPropertiesConfigAdapter extends PropertiesConfigAdapter<StatsdProperties> implements StatsdConfig {

	/**
     * Constructs a new StatsdPropertiesConfigAdapter with the specified StatsdProperties.
     * 
     * @param properties the StatsdProperties to be used for configuring the adapter
     */
    public StatsdPropertiesConfigAdapter(StatsdProperties properties) {
		super(properties);
	}

	/**
     * Retrieves the value associated with the specified key from the StatsdPropertiesConfigAdapter.
     *
     * @param s the key whose associated value is to be retrieved
     * @return the value to which the specified key is mapped, or null if the key is not found
     */
    @Override
	public String get(String s) {
		return null;
	}

	/**
     * Returns the prefix used for exporting metrics to StatsD.
     *
     * @return the prefix used for exporting metrics to StatsD
     */
    @Override
	public String prefix() {
		return "management.statsd.metrics.export";
	}

	/**
     * Returns the flavor of the Statsd configuration.
     * 
     * @return the flavor of the Statsd configuration
     */
    @Override
	public StatsdFlavor flavor() {
		return get(StatsdProperties::getFlavor, StatsdConfig.super::flavor);
	}

	/**
     * Returns whether the Statsd properties are enabled.
     * 
     * @return {@code true} if the Statsd properties are enabled, {@code false} otherwise.
     */
    @Override
	public boolean enabled() {
		return get(StatsdProperties::isEnabled, StatsdConfig.super::enabled);
	}

	/**
     * Returns the host for the Statsd server.
     * 
     * @return the host for the Statsd server
     */
    @Override
	public String host() {
		return get(StatsdProperties::getHost, StatsdConfig.super::host);
	}

	/**
     * Returns the port number for the Statsd server.
     *
     * @return the port number
     */
    @Override
	public int port() {
		return get(StatsdProperties::getPort, StatsdConfig.super::port);
	}

	/**
     * Returns the protocol used for Statsd communication.
     * 
     * @return the protocol used for Statsd communication
     */
    @Override
	public StatsdProtocol protocol() {
		return get(StatsdProperties::getProtocol, StatsdConfig.super::protocol);
	}

	/**
     * Returns the maximum packet length.
     *
     * @return the maximum packet length
     */
    @Override
	public int maxPacketLength() {
		return get(StatsdProperties::getMaxPacketLength, StatsdConfig.super::maxPacketLength);
	}

	/**
     * Returns the polling frequency for the StatsdConfig.
     * 
     * @return the polling frequency
     */
    @Override
	public Duration pollingFrequency() {
		return get(StatsdProperties::getPollingFrequency, StatsdConfig.super::pollingFrequency);
	}

	/**
     * Returns the step duration for the StatsdConfig.
     * 
     * @return the step duration
     */
    @Override
	public Duration step() {
		return get(StatsdProperties::getStep, StatsdConfig.super::step);
	}

	/**
     * Returns a boolean value indicating whether unchanged meters should be published.
     * 
     * @return true if unchanged meters should be published, false otherwise
     */
    @Override
	public boolean publishUnchangedMeters() {
		return get(StatsdProperties::isPublishUnchangedMeters, StatsdConfig.super::publishUnchangedMeters);
	}

	/**
     * Returns a boolean value indicating whether the statsd client is buffered.
     * 
     * @return {@code true} if the statsd client is buffered, {@code false} otherwise
     */
    @Override
	public boolean buffered() {
		return get(StatsdProperties::isBuffered, StatsdConfig.super::buffered);
	}

}
