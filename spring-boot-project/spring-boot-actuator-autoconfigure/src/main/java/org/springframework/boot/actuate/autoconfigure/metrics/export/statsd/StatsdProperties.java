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

import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdProtocol;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring StatsD metrics
 * export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.statsd.metrics.export")
public class StatsdProperties {

	/**
	 * Whether exporting of metrics to StatsD is enabled.
	 */
	private boolean enabled = true;

	/**
	 * StatsD line protocol to use.
	 */
	private StatsdFlavor flavor = StatsdFlavor.DATADOG;

	/**
	 * Host of the StatsD server to receive exported metrics.
	 */
	private String host = "localhost";

	/**
	 * Port of the StatsD server to receive exported metrics.
	 */
	private Integer port = 8125;

	/**
	 * Protocol of the StatsD server to receive exported metrics.
	 */
	private StatsdProtocol protocol = StatsdProtocol.UDP;

	/**
	 * Total length of a single payload should be kept within your network's MTU.
	 */
	private Integer maxPacketLength = 1400;

	/**
	 * How often gauges will be polled. When a gauge is polled, its value is recalculated
	 * and if the value has changed (or publishUnchangedMeters is true), it is sent to the
	 * StatsD server.
	 */
	private Duration pollingFrequency = Duration.ofSeconds(10);

	/**
	 * Step size to use in computing windowed statistics like max. To get the most out of
	 * these statistics, align the step interval to be close to your scrape interval.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
	 * Whether to send unchanged meters to the StatsD server.
	 */
	private boolean publishUnchangedMeters = true;

	/**
	 * Whether measurements should be buffered before sending to the StatsD server.
	 */
	private boolean buffered = true;

	/**
	 * Returns the current status of the enabled flag.
	 * @return true if the enabled flag is set to true, false otherwise.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Sets the enabled status of the StatsdProperties.
	 * @param enabled the enabled status to be set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the flavor of the Statsd server.
	 * @return the flavor of the Statsd server
	 */
	public StatsdFlavor getFlavor() {
		return this.flavor;
	}

	/**
	 * Sets the flavor of the Statsd client.
	 * @param flavor the flavor to set
	 */
	public void setFlavor(StatsdFlavor flavor) {
		this.flavor = flavor;
	}

	/**
	 * Returns the host value of the StatsdProperties object.
	 * @return the host value of the StatsdProperties object
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host for the StatsdProperties.
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns the port number for the Statsd server.
	 * @return the port number for the Statsd server
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port for StatsdProperties.
	 * @param port the port to be set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Returns the protocol used by the StatsdProperties.
	 * @return the protocol used by the StatsdProperties
	 */
	public StatsdProtocol getProtocol() {
		return this.protocol;
	}

	/**
	 * Sets the protocol for sending data to Statsd.
	 * @param protocol the protocol to be set
	 */
	public void setProtocol(StatsdProtocol protocol) {
		this.protocol = protocol;
	}

	/**
	 * Returns the maximum packet length.
	 * @return the maximum packet length
	 */
	public Integer getMaxPacketLength() {
		return this.maxPacketLength;
	}

	/**
	 * Sets the maximum packet length for StatsdProperties.
	 * @param maxPacketLength the maximum packet length to be set
	 */
	public void setMaxPacketLength(Integer maxPacketLength) {
		this.maxPacketLength = maxPacketLength;
	}

	/**
	 * Returns the polling frequency for the StatsdProperties.
	 * @return the polling frequency as a Duration object
	 */
	public Duration getPollingFrequency() {
		return this.pollingFrequency;
	}

	/**
	 * Sets the polling frequency for the StatsdProperties.
	 * @param pollingFrequency the duration representing the polling frequency
	 */
	public void setPollingFrequency(Duration pollingFrequency) {
		this.pollingFrequency = pollingFrequency;
	}

	/**
	 * Returns the step duration.
	 * @return the step duration
	 */
	public Duration getStep() {
		return this.step;
	}

	/**
	 * Sets the step duration for sending metrics to StatsD.
	 * @param step the duration of the step
	 */
	public void setStep(Duration step) {
		this.step = step;
	}

	/**
	 * Returns the value of the publishUnchangedMeters property.
	 * @return true if unchanged meters should be published, false otherwise
	 */
	public boolean isPublishUnchangedMeters() {
		return this.publishUnchangedMeters;
	}

	/**
	 * Sets the flag to determine whether unchanged meters should be published.
	 * @param publishUnchangedMeters true if unchanged meters should be published, false
	 * otherwise
	 */
	public void setPublishUnchangedMeters(boolean publishUnchangedMeters) {
		this.publishUnchangedMeters = publishUnchangedMeters;
	}

	/**
	 * Returns a boolean value indicating whether the StatsdProperties object is buffered.
	 * @return true if the StatsdProperties object is buffered, false otherwise
	 */
	public boolean isBuffered() {
		return this.buffered;
	}

	/**
	 * Sets the value indicating whether the data should be buffered before sending to the
	 * StatsD server.
	 * @param buffered true if the data should be buffered, false otherwise
	 */
	public void setBuffered(boolean buffered) {
		this.buffered = buffered;
	}

}
