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

import io.micrometer.graphite.GraphiteProtocol;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ObjectUtils;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Graphite
 * metrics export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.graphite.metrics.export")
public class GraphiteProperties {

	/**
	 * Whether exporting of metrics to Graphite is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
	 * Base time unit used to report rates.
	 */
	private TimeUnit rateUnits = TimeUnit.SECONDS;

	/**
	 * Base time unit used to report durations.
	 */
	private TimeUnit durationUnits = TimeUnit.MILLISECONDS;

	/**
	 * Host of the Graphite server to receive exported metrics.
	 */
	private String host = "localhost";

	/**
	 * Port of the Graphite server to receive exported metrics.
	 */
	private Integer port = 2004;

	/**
	 * Protocol to use while shipping data to Graphite.
	 */
	private GraphiteProtocol protocol = GraphiteProtocol.PICKLED;

	/**
	 * Whether Graphite tags should be used, as opposed to a hierarchical naming
	 * convention. Enabled by default unless "tagsAsPrefix" is set.
	 */
	private Boolean graphiteTagsEnabled;

	/**
	 * For the hierarchical naming convention, turn the specified tag keys into part of
	 * the metric prefix. Ignored if "graphiteTagsEnabled" is true.
	 */
	private String[] tagsAsPrefix = new String[0];

	/**
     * Returns the current status of the enabled flag.
     * 
     * @return true if the enabled flag is set to true, false otherwise.
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the GraphiteProperties.
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
     * Sets the step duration for the GraphiteProperties.
     * 
     * @param step the step duration to be set
     */
    public void setStep(Duration step) {
		this.step = step;
	}

	/**
     * Returns the rate units of the GraphiteProperties.
     *
     * @return the rate units of the GraphiteProperties
     */
    public TimeUnit getRateUnits() {
		return this.rateUnits;
	}

	/**
     * Sets the rate units for the GraphiteProperties.
     * 
     * @param rateUnits the rate units to be set
     */
    public void setRateUnits(TimeUnit rateUnits) {
		this.rateUnits = rateUnits;
	}

	/**
     * Returns the duration units of the GraphiteProperties.
     *
     * @return the duration units of the GraphiteProperties
     */
    public TimeUnit getDurationUnits() {
		return this.durationUnits;
	}

	/**
     * Sets the duration units for the GraphiteProperties.
     * 
     * @param durationUnits the duration units to be set
     */
    public void setDurationUnits(TimeUnit durationUnits) {
		this.durationUnits = durationUnits;
	}

	/**
     * Returns the host value of the GraphiteProperties object.
     *
     * @return the host value of the GraphiteProperties object
     */
    public String getHost() {
		return this.host;
	}

	/**
     * Sets the host for the GraphiteProperties.
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
     * Sets the port number for the Graphite server.
     * 
     * @param port the port number to set
     */
    public void setPort(Integer port) {
		this.port = port;
	}

	/**
     * Returns the GraphiteProtocol associated with this GraphiteProperties instance.
     *
     * @return the GraphiteProtocol associated with this GraphiteProperties instance
     */
    public GraphiteProtocol getProtocol() {
		return this.protocol;
	}

	/**
     * Sets the protocol for connecting to Graphite.
     * 
     * @param protocol the GraphiteProtocol to set
     */
    public void setProtocol(GraphiteProtocol protocol) {
		this.protocol = protocol;
	}

	/**
     * Returns the value indicating whether the graphite tags are enabled.
     * 
     * @return {@code true} if the graphite tags are enabled, {@code false} otherwise
     */
    public Boolean getGraphiteTagsEnabled() {
		return (this.graphiteTagsEnabled != null) ? this.graphiteTagsEnabled : ObjectUtils.isEmpty(this.tagsAsPrefix);
	}

	/**
     * Sets the flag indicating whether Graphite tags are enabled.
     * 
     * @param graphiteTagsEnabled the flag indicating whether Graphite tags are enabled
     */
    public void setGraphiteTagsEnabled(Boolean graphiteTagsEnabled) {
		this.graphiteTagsEnabled = graphiteTagsEnabled;
	}

	/**
     * Returns the tags as prefix.
     * 
     * @return the tags as prefix
     */
    public String[] getTagsAsPrefix() {
		return this.tagsAsPrefix;
	}

	/**
     * Sets the tags as prefix for the GraphiteProperties.
     * 
     * @param tagsAsPrefix the array of tags to be set as prefix
     */
    public void setTagsAsPrefix(String[] tagsAsPrefix) {
		this.tagsAsPrefix = tagsAsPrefix;
	}

}
