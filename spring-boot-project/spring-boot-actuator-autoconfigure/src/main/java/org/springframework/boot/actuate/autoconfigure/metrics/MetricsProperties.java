/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties("management.metrics")
public class MetricsProperties {

	private final Web web = new Web();

	private final Summaries summaries = new Summaries();

	private final Timers timers = new Timers();

	/**
	 * If {@code false}, the matching meter(s) are no-op.
	 */
	private Map<String, Boolean> enabled = new HashMap<>();

	/**
	 * Whether auto-configured MeterRegistry implementations should be bound to the global
	 * static registry on Metrics. For testing, set this to 'false' to maximize test
	 * independence.
	 */
	private boolean useGlobalRegistry = true;

	public boolean isUseGlobalRegistry() {
		return this.useGlobalRegistry;
	}

	public void setUseGlobalRegistry(boolean useGlobalRegistry) {
		this.useGlobalRegistry = useGlobalRegistry;
	}

	public Web getWeb() {
		return this.web;
	}

	public Summaries getSummaries() {
		return this.summaries;
	}

	public Timers getTimers() {
		return this.timers;
	}

	public Map<String, Boolean> getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Map<String, Boolean> enabled) {
		this.enabled = enabled;
	}

	/**
	 * Configuration of server and client request metrics.
	 */
	public static class Web {

		private final Client client = new Client();

		private final Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public Server getServer() {
			return this.server;
		}

		/**
		 * Configuration of client request metrics.
		 */
		public static class Client {

			/**
			 * Name of the metric for sent requests.
			 */
			private String requestsMetricName = "http.client.requests";

			public String getRequestsMetricName() {
				return this.requestsMetricName;
			}

			public void setRequestsMetricName(String requestsMetricName) {
				this.requestsMetricName = requestsMetricName;
			}

		}

		/**
		 * Configuration of server request metrics.
		 */
		public static class Server {

			/**
			 * Whether requests handled by Spring MVC or WebFlux should be automatically
			 * timed. If the number of time series emitted grows too large on account of
			 * request mapping timings, disable this and use 'Timed' on a per request
			 * mapping basis as needed.
			 */
			private boolean autoTimeRequests = true;

			/**
			 * Name of the metric for received requests.
			 */
			private String requestsMetricName = "http.server.requests";

			public boolean isAutoTimeRequests() {
				return this.autoTimeRequests;
			}

			public void setAutoTimeRequests(boolean autoTimeRequests) {
				this.autoTimeRequests = autoTimeRequests;
			}

			public String getRequestsMetricName() {
				return this.requestsMetricName;
			}

			public void setRequestsMetricName(String requestsMetricName) {
				this.requestsMetricName = requestsMetricName;
			}

		}

	}

	/**
	 * Properties common to "distribution" style meters - timers and distribution summaries.
	 */
	static abstract class AbstractDistributions {
		/**
		 * Controls whether to publish a histogram structure for those monitoring systems that support
		 * aggregable percentile calculation based on a histogram. For other systems, this has no effect.
		 */
		private Map<String, Boolean> percentileHistogram = new HashMap<>();

		/**
		 * The set of Micrometer-computed non-aggregable percentiles to ship to the backend. Percentiles should
		 * be defined in the range of (0, 1]. For example, 0.999 represents the 99.9th percentile of the distribution.
		 */
		private Map<String, double[]> percentiles = new HashMap<>();

		/**
		 * Statistics emanating from a distribution like max, percentiles, and histogram counts decay over time to
		 * give greater weight to recent samples (exception: histogram counts are cumulative for those systems that expect cumulative
		 * histogram buckets). Samples are accumulated to such statistics in ring buffers which rotate after
		 * this expiry, with a buffer length of {@link #histogramBufferLength}.
		 */
		private Map<String, Duration> histogramExpiry = new HashMap<>();

		/**
		 * Statistics emanating from a distribution like max, percentiles, and histogram counts decay over time to
		 * give greater weight to recent samples (exception: histogram counts are cumulative for those systems that expect cumulative
		 * histogram buckets). Samples are accumulated to such statistics in ring buffers which rotate after
		 * {@link #histogramExpiry}, with this buffer length.
		 */
		private Map<String, Integer> histogramBufferLength = new HashMap<>();

		public Map<String, Boolean> getPercentileHistogram() {
			return this.percentileHistogram;
		}

		public void setPercentileHistogram(Map<String, Boolean> percentileHistogram) {
			this.percentileHistogram = percentileHistogram;
		}

		public Map<String, double[]> getPercentiles() {
			return this.percentiles;
		}

		public void setPercentiles(Map<String, double[]> percentiles) {
			this.percentiles = percentiles;
		}

		public Map<String, Duration> getHistogramExpiry() {
			return this.histogramExpiry;
		}

		public void setHistogramExpiry(Map<String, Duration> histogramExpiry) {
			this.histogramExpiry = histogramExpiry;
		}

		public Map<String, Integer> getHistogramBufferLength() {
			return this.histogramBufferLength;
		}

		public void setHistogramBufferLength(Map<String, Integer> histogramBufferLength) {
			this.histogramBufferLength = histogramBufferLength;
		}
	}

	/**
	 * {@link MeterFilter} configuration of {@link DistributionSummary}s.
	 */
	public static class Summaries extends AbstractDistributions {
		/**
		 * Clamps {@link DistributionSummary} to the first percentile bucket greater than
		 * or equal to the supplied value. Use this property to control the number of histogram buckets used
		 * to represent a distribution.
		 */
		private Map<String, Long> minimumExpectedValue = new HashMap<>();

		/**
		 * Clamps {@link DistributionSummary} to the percentile buckets less than
		 * or equal to the supplied value. Use this property to control the number of histogram buckets used
		 * to represent a distribution.
		 */
		private Map<String, Long> maximumExpectedValue = new HashMap<>();

		/**
		 * Publish a counter for each SLA boundary that counts violations of the SLA.
		 */
		private Map<String, long[]> sla = new HashMap<>();

		public Map<String, Long> getMinimumExpectedValue() {
			return this.minimumExpectedValue;
		}

		public void setMinimumExpectedValue(Map<String, Long> minimumExpectedValue) {
			this.minimumExpectedValue = minimumExpectedValue;
		}

		public Map<String, Long> getMaximumExpectedValue() {
			return this.maximumExpectedValue;
		}

		public void setMaximumExpectedValue(Map<String, Long> maximumExpectedValue) {
			this.maximumExpectedValue = maximumExpectedValue;
		}

		public Map<String, long[]> getSla() {
			return this.sla;
		}

		public void setSla(Map<String, long[]> sla) {
			this.sla = sla;
		}
	}

	/**
	 * {@link MeterFilter} configuration of {@link Timer}s.
	 */
	public static class Timers extends AbstractDistributions {
		/**
		 * Clamps {@link Timer} to the first percentile bucket greater than
		 * or equal to the supplied value. Use this property to control the number of histogram buckets used
		 * to represent a distribution.
		 */
		Map<String, Duration> minimumExpectedValue = new HashMap<>();

		/**
		 * Clamps {@link Timer} to the percentile buckets less than
		 * or equal to the supplied value. Use this property to control the number of histogram buckets used
		 * to represent a distribution.
		 */
		Map<String, Duration> maximumExpectedValue = new HashMap<>();

		/**
		 * Publish a counter for each SLA boundary that counts violations of the SLA.
		 */
		Map<String, Duration[]> sla = new HashMap<>();

		public Map<String, Duration> getMinimumExpectedValue() {
			return this.minimumExpectedValue;
		}

		public void setMinimumExpectedValue(Map<String, Duration> minimumExpectedValue) {
			this.minimumExpectedValue = minimumExpectedValue;
		}

		public Map<String, Duration> getMaximumExpectedValue() {
			return this.maximumExpectedValue;
		}

		public void setMaximumExpectedValue(Map<String, Duration> maximumExpectedValue) {
			this.maximumExpectedValue = maximumExpectedValue;
		}

		public Map<String, Duration[]> getSla() {
			return this.sla;
		}

		public void setSla(Map<String, Duration[]> sla) {
			this.sla = sla;
		}
	}
}
