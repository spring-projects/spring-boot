/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.micrometer.prometheus.HistogramFlavor;

import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager.ShutdownOperation;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring metrics export
 * to Prometheus.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.prometheus.metrics.export")
public class PrometheusProperties {

	/**
	 * Whether exporting of metrics to this backend is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Whether to enable publishing descriptions as part of the scrape payload to
	 * Prometheus. Turn this off to minimize the amount of data sent on each scrape.
	 */
	private boolean descriptions = true;

	/**
	 * Configuration options for using Prometheus Pushgateway, allowing metrics to be
	 * pushed when they cannot be scraped.
	 */
	private final Pushgateway pushgateway = new Pushgateway();

	/**
	 * Histogram type for backing DistributionSummary and Timer.
	 */
	private HistogramFlavor histogramFlavor = HistogramFlavor.Prometheus;

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
     * Returns the value indicating whether descriptions are enabled or not.
     * 
     * @return true if descriptions are enabled, false otherwise
     */
    public boolean isDescriptions() {
		return this.descriptions;
	}

	/**
     * Sets the value of the descriptions property.
     * 
     * @param descriptions the new value for the descriptions property
     */
    public void setDescriptions(boolean descriptions) {
		this.descriptions = descriptions;
	}

	/**
     * Returns the histogram flavor used by the PrometheusProperties class.
     * 
     * @return the histogram flavor used by the PrometheusProperties class
     */
    public HistogramFlavor getHistogramFlavor() {
		return this.histogramFlavor;
	}

	/**
     * Sets the histogram flavor for the PrometheusProperties class.
     * 
     * @param histogramFlavor the histogram flavor to be set
     */
    public void setHistogramFlavor(HistogramFlavor histogramFlavor) {
		this.histogramFlavor = histogramFlavor;
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
     * Sets the step duration for data collection.
     * 
     * @param step the duration of each step in data collection
     */
    public void setStep(Duration step) {
		this.step = step;
	}

	/**
     * Returns the current status of the enabled flag.
     *
     * @return true if the enabled flag is set to true, false otherwise.
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the PrometheusProperties.
     * 
     * @param enabled the enabled status to be set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns the Pushgateway instance associated with this PrometheusProperties object.
     *
     * @return the Pushgateway instance
     */
    public Pushgateway getPushgateway() {
		return this.pushgateway;
	}

	/**
	 * Configuration options for push-based interaction with Prometheus.
	 */
	public static class Pushgateway {

		/**
		 * Enable publishing over a Prometheus Pushgateway.
		 */
		private Boolean enabled = false;

		/**
		 * Base URL for the Pushgateway.
		 */
		private String baseUrl = "http://localhost:9091";

		/**
		 * Login user of the Prometheus Pushgateway.
		 */
		private String username;

		/**
		 * Login password of the Prometheus Pushgateway.
		 */
		private String password;

		/**
		 * Frequency with which to push metrics.
		 */
		private Duration pushRate = Duration.ofMinutes(1);

		/**
		 * Job identifier for this application instance.
		 */
		private String job;

		/**
		 * Grouping key for the pushed metrics.
		 */
		private Map<String, String> groupingKey = new HashMap<>();

		/**
		 * Operation that should be performed on shutdown.
		 */
		private ShutdownOperation shutdownOperation = ShutdownOperation.NONE;

		/**
         * Returns the value of the 'enabled' property.
         *
         * @return the value of the 'enabled' property
         */
        public Boolean getEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Pushgateway.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the base URL of the Pushgateway.
         *
         * @return the base URL of the Pushgateway
         */
        public String getBaseUrl() {
			return this.baseUrl;
		}

		/**
         * Sets the base URL for the Pushgateway.
         * 
         * @param baseUrl the base URL to be set
         */
        public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		/**
         * Returns the username associated with the Pushgateway.
         *
         * @return the username associated with the Pushgateway
         */
        public String getUsername() {
			return this.username;
		}

		/**
         * Sets the username for authentication.
         * 
         * @param username the username to be set
         */
        public void setUsername(String username) {
			this.username = username;
		}

		/**
         * Returns the password associated with the Pushgateway.
         *
         * @return the password associated with the Pushgateway
         */
        public String getPassword() {
			return this.password;
		}

		/**
         * Sets the password for the Pushgateway.
         * 
         * @param password the password to be set
         */
        public void setPassword(String password) {
			this.password = password;
		}

		/**
         * Returns the push rate of the Pushgateway.
         *
         * @return the push rate of the Pushgateway
         */
        public Duration getPushRate() {
			return this.pushRate;
		}

		/**
         * Sets the push rate for the Pushgateway.
         * 
         * @param pushRate the duration representing the push rate
         */
        public void setPushRate(Duration pushRate) {
			this.pushRate = pushRate;
		}

		/**
         * Returns the job associated with the Pushgateway.
         *
         * @return the job associated with the Pushgateway
         */
        public String getJob() {
			return this.job;
		}

		/**
         * Sets the job for the Pushgateway.
         * 
         * @param job the job to be set for the Pushgateway
         */
        public void setJob(String job) {
			this.job = job;
		}

		/**
         * Returns the grouping key of the Pushgateway.
         * 
         * @return the grouping key as a Map<String, String>
         */
        public Map<String, String> getGroupingKey() {
			return this.groupingKey;
		}

		/**
         * Sets the grouping key for the Pushgateway.
         * 
         * @param groupingKey the grouping key to be set, represented as a Map of String keys and String values
         */
        public void setGroupingKey(Map<String, String> groupingKey) {
			this.groupingKey = groupingKey;
		}

		/**
         * Returns the shutdown operation associated with this Pushgateway.
         *
         * @return the shutdown operation associated with this Pushgateway
         */
        public ShutdownOperation getShutdownOperation() {
			return this.shutdownOperation;
		}

		/**
         * Sets the shutdown operation for the Pushgateway.
         * 
         * @param shutdownOperation the shutdown operation to be set
         */
        public void setShutdownOperation(ShutdownOperation shutdownOperation) {
			this.shutdownOperation = shutdownOperation;
		}

	}

}
