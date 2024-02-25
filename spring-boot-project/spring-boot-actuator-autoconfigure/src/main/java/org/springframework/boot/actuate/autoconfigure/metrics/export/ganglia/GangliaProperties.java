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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Ganglia
 * metrics export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.ganglia.metrics.export")
public class GangliaProperties {

	/**
	 * Whether exporting of metrics to Ganglia is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
	 * Base time unit used to report durations.
	 */
	private TimeUnit durationUnits = TimeUnit.MILLISECONDS;

	/**
	 * UDP addressing mode, either unicast or multicast.
	 */
	private GMetric.UDPAddressingMode addressingMode = GMetric.UDPAddressingMode.MULTICAST;

	/**
	 * Time to live for metrics on Ganglia. Set the multicast Time-To-Live to be one
	 * greater than the number of hops (routers) between the hosts.
	 */
	private Integer timeToLive = 1;

	/**
	 * Host of the Ganglia server to receive exported metrics.
	 */
	private String host = "localhost";

	/**
	 * Port of the Ganglia server to receive exported metrics.
	 */
	private Integer port = 8649;

	/**
     * Returns the current status of the enabled flag.
     *
     * @return true if the enabled flag is set to true, false otherwise.
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the GangliaProperties.
     * 
     * @param enabled the enabled status to be set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns the step duration.
     *
     * @return the step duration
     */
    public Duration getStep() {
		return this.step;
	}

	/**
     * Sets the step duration for GangliaProperties.
     * 
     * @param step the step duration to be set
     */
    public void setStep(Duration step) {
		this.step = step;
	}

	/**
     * Returns the duration units used in the GangliaProperties class.
     * 
     * @return the duration units used in the GangliaProperties class
     */
    public TimeUnit getDurationUnits() {
		return this.durationUnits;
	}

	/**
     * Sets the duration units for the GangliaProperties.
     * 
     * @param durationUnits the duration units to be set
     */
    public void setDurationUnits(TimeUnit durationUnits) {
		this.durationUnits = durationUnits;
	}

	/**
     * Returns the addressing mode used for UDP communication in Ganglia.
     *
     * @return the addressing mode used for UDP communication
     */
    public GMetric.UDPAddressingMode getAddressingMode() {
		return this.addressingMode;
	}

	/**
     * Sets the addressing mode for the GMetric UDP connection.
     * 
     * @param addressingMode the addressing mode to be set
     */
    public void setAddressingMode(GMetric.UDPAddressingMode addressingMode) {
		this.addressingMode = addressingMode;
	}

	/**
     * Returns the time to live value.
     *
     * @return the time to live value
     */
    public Integer getTimeToLive() {
		return this.timeToLive;
	}

	/**
     * Sets the time to live for the GangliaProperties.
     * 
     * @param timeToLive the time to live value to be set
     */
    public void setTimeToLive(Integer timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
     * Returns the host value.
     *
     * @return the host value
     */
    public String getHost() {
		return this.host;
	}

	/**
     * Sets the host for the GangliaProperties.
     * 
     * @param host the host to be set
     */
    public void setHost(String host) {
		this.host = host;
	}

	/**
     * Returns the port number.
     *
     * @return the port number
     */
    public Integer getPort() {
		return this.port;
	}

	/**
     * Sets the port number for the GangliaProperties.
     * 
     * @param port the port number to be set
     */
    public void setPort(Integer port) {
		this.port = port;
	}

}
