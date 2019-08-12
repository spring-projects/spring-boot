/*
 * Copyright 2012-2019 the original author or authors.
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
@ConfigurationProperties(prefix = "management.metrics.export.prometheus")
public class PrometheusProperties {

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
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	public boolean isDescriptions() {
		return this.descriptions;
	}

	public void setDescriptions(boolean descriptions) {
		this.descriptions = descriptions;
	}

	public Duration getStep() {
		return this.step;
	}

	public void setStep(Duration step) {
		this.step = step;
	}

	public Pushgateway getPushgateway() {
		return this.pushgateway;
	}

	/**
	 * Configuration options for push-based interaction with Prometheus.
	 */
	public static class Pushgateway {

		/**
		 * Enable publishing via a Prometheus Pushgateway.
		 */
		private Boolean enabled = false;

		/**
		 * Base URL for the Pushgateway.
		 */
		private String baseUrl = "http://localhost:9091";

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

		public Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public String getBaseUrl() {
			return this.baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public Duration getPushRate() {
			return this.pushRate;
		}

		public void setPushRate(Duration pushRate) {
			this.pushRate = pushRate;
		}

		public String getJob() {
			return this.job;
		}

		public void setJob(String job) {
			this.job = job;
		}

		public Map<String, String> getGroupingKey() {
			return this.groupingKey;
		}

		public void setGroupingKey(Map<String, String> groupingKey) {
			this.groupingKey = groupingKey;
		}

		public ShutdownOperation getShutdownOperation() {
			return this.shutdownOperation;
		}

		public void setShutdownOperation(ShutdownOperation shutdownOperation) {
			this.shutdownOperation = shutdownOperation;
		}

	}

}
