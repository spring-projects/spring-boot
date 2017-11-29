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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import java.time.Duration;

import io.micrometer.statsd.StatsdFlavor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring StatsD metrics export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.export.statsd")
public class StatsdProperties {

	/**
	 * Export metrics to StatsD.
	 */
	private Boolean enabled;

	/**
	 * StatsD line protocol to use.
	 */
	private StatsdFlavor flavor = StatsdFlavor.Datadog;

	/**
	 * Host of the StatsD server to receive exported metrics.
	 */
	private String host = "localhost";

	/**
	 * Port of the StatsD server to receive exported metrics.
	 */
	private Integer port = 8125;

	/**
	 * Total length of a single payload should be kept within your network's MTU.
	 */
	private Integer maxPacketLength = 1400;

	/**
	 * How often gauges will be polled. When a gauge is polled, its value is
	 * recalculated and if the value has changed, it is sent to the StatsD server.
	 */
	private Duration pollingFrequency = Duration.ofSeconds(10);

	/**
	 * Maximum size of the queue of items waiting to be sent to the StatsD server.
	 */
	private Integer queueSize = Integer.MAX_VALUE;

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public StatsdFlavor getFlavor() {
		return this.flavor;
	}

	public void setFlavor(StatsdFlavor flavor) {
		this.flavor = flavor;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getMaxPacketLength() {
		return this.maxPacketLength;
	}

	public void setMaxPacketLength(Integer maxPacketLength) {
		this.maxPacketLength = maxPacketLength;
	}

	public Duration getPollingFrequency() {
		return this.pollingFrequency;
	}

	public void setPollingFrequency(Duration pollingFrequency) {
		this.pollingFrequency = pollingFrequency;
	}

	public Integer getQueueSize() {
		return this.queueSize;
	}

	public void setQueueSize(Integer queueSize) {
		this.queueSize = queueSize;
	}

}
