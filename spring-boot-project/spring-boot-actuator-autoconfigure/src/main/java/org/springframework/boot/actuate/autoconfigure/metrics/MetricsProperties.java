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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring
 * Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @author Alexander Abramov
 * @author Tadaya Tsuyukubo
 * @author Chris Bono
 * @since 2.0.0
 */
@ConfigurationProperties("management.metrics")
public class MetricsProperties {

	/**
	 * Whether auto-configured MeterRegistry implementations should be bound to the global
	 * static registry on Metrics. For testing, set this to 'false' to maximize test
	 * independence.
	 */
	private boolean useGlobalRegistry = true;

	/**
	 * Whether meter IDs starting with the specified name should be enabled. The longest
	 * match wins, the key 'all' can also be used to configure all meters.
	 */
	private final Map<String, Boolean> enable = new LinkedHashMap<>();

	/**
	 * Common tags that are applied to every meter.
	 */
	private final Map<String, String> tags = new LinkedHashMap<>();

	private final Web web = new Web();

	private final Data data = new Data();

	private final System system = new System();

	private final Distribution distribution = new Distribution();

	/**
	 * Returns a boolean value indicating whether the global registry is being used.
	 * @return true if the global registry is being used, false otherwise
	 */
	public boolean isUseGlobalRegistry() {
		return this.useGlobalRegistry;
	}

	/**
	 * Sets whether to use the global registry for metrics.
	 * @param useGlobalRegistry true to use the global registry, false otherwise
	 */
	public void setUseGlobalRegistry(boolean useGlobalRegistry) {
		this.useGlobalRegistry = useGlobalRegistry;
	}

	/**
	 * Returns a map of enable values.
	 * @return a map containing enable values
	 */
	public Map<String, Boolean> getEnable() {
		return this.enable;
	}

	/**
	 * Returns the tags associated with the MetricsProperties object.
	 * @return a Map containing the tags as key-value pairs
	 */
	public Map<String, String> getTags() {
		return this.tags;
	}

	/**
	 * Returns the Web object associated with this MetricsProperties instance.
	 * @return the Web object
	 */
	public Web getWeb() {
		return this.web;
	}

	/**
	 * Returns the data object.
	 * @return the data object
	 */
	public Data getData() {
		return this.data;
	}

	/**
	 * Returns the system object associated with this MetricsProperties instance.
	 * @return the system object
	 */
	public System getSystem() {
		return this.system;
	}

	/**
	 * Returns the distribution of the MetricsProperties.
	 * @return the distribution of the MetricsProperties
	 */
	public Distribution getDistribution() {
		return this.distribution;
	}

	/**
	 * Web class.
	 */
	public static class Web {

		private final Client client = new Client();

		private final Server server = new Server();

		/**
		 * Returns the client associated with this Web instance.
		 * @return the client associated with this Web instance
		 */
		public Client getClient() {
			return this.client;
		}

		/**
		 * Returns the server object associated with this Web class.
		 * @return the server object
		 */
		public Server getServer() {
			return this.server;
		}

		/**
		 * Client class.
		 */
		public static class Client {

			/**
			 * Maximum number of unique URI tag values allowed. After the max number of
			 * tag values is reached, metrics with additional tag values are denied by
			 * filter.
			 */
			private int maxUriTags = 100;

			/**
			 * Returns the maximum number of URI tags.
			 * @return the maximum number of URI tags
			 */
			public int getMaxUriTags() {
				return this.maxUriTags;
			}

			/**
			 * Sets the maximum number of URI tags.
			 * @param maxUriTags the maximum number of URI tags to be set
			 */
			public void setMaxUriTags(int maxUriTags) {
				this.maxUriTags = maxUriTags;
			}

		}

		/**
		 * Server class.
		 */
		public static class Server {

			/**
			 * Maximum number of unique URI tag values allowed. After the max number of
			 * tag values is reached, metrics with additional tag values are denied by
			 * filter.
			 */
			private int maxUriTags = 100;

			/**
			 * Returns the maximum number of URI tags.
			 * @return the maximum number of URI tags
			 */
			public int getMaxUriTags() {
				return this.maxUriTags;
			}

			/**
			 * Sets the maximum number of URI tags.
			 * @param maxUriTags the maximum number of URI tags to be set
			 */
			public void setMaxUriTags(int maxUriTags) {
				this.maxUriTags = maxUriTags;
			}

		}

	}

	/**
	 * Data class.
	 */
	public static class Data {

		private final Repository repository = new Repository();

		/**
		 * Returns the repository associated with this Data object.
		 * @return the repository associated with this Data object
		 */
		public Repository getRepository() {
			return this.repository;
		}

		/**
		 * Repository class.
		 */
		public static class Repository {

			/**
			 * Name of the metric for sent requests.
			 */
			private String metricName = "spring.data.repository.invocations";

			/**
			 * Auto-timed request settings.
			 */
			@NestedConfigurationProperty
			private final AutoTimeProperties autotime = new AutoTimeProperties();

			/**
			 * Returns the metric name.
			 * @return the metric name
			 */
			public String getMetricName() {
				return this.metricName;
			}

			/**
			 * Sets the metric name for the repository.
			 * @param metricName the metric name to be set
			 */
			public void setMetricName(String metricName) {
				this.metricName = metricName;
			}

			/**
			 * Returns the AutoTimeProperties object associated with this Repository.
			 * @return the AutoTimeProperties object associated with this Repository
			 */
			public AutoTimeProperties getAutotime() {
				return this.autotime;
			}

		}

	}

	/**
	 * System class.
	 */
	public static class System {

		private final Diskspace diskspace = new Diskspace();

		/**
		 * Returns the diskspace object associated with the system.
		 * @return the diskspace object associated with the system
		 */
		public Diskspace getDiskspace() {
			return this.diskspace;
		}

		/**
		 * Diskspace class.
		 */
		public static class Diskspace {

			/**
			 * Comma-separated list of paths to report disk metrics for.
			 */
			private List<File> paths = new ArrayList<>(Collections.singletonList(new File(".")));

			/**
			 * Returns a list of File objects representing the paths.
			 * @return a list of File objects representing the paths
			 */
			public List<File> getPaths() {
				return this.paths;
			}

			/**
			 * Sets the paths for the Diskspace object.
			 * @param paths the list of File objects representing the paths to be set
			 */
			public void setPaths(List<File> paths) {
				this.paths = paths;
			}

		}

	}

	/**
	 * Distribution class.
	 */
	public static class Distribution {

		/**
		 * Whether meter IDs starting with the specified name should publish percentile
		 * histograms. For monitoring systems that support aggregable percentile
		 * calculation based on a histogram, this can be set to true. For other systems,
		 * this has no effect. The longest match wins, the key 'all' can also be used to
		 * configure all meters.
		 */
		private final Map<String, Boolean> percentilesHistogram = new LinkedHashMap<>();

		/**
		 * Specific computed non-aggregable percentiles to ship to the backend for meter
		 * IDs starting-with the specified name. The longest match wins, the key 'all' can
		 * also be used to configure all meters.
		 */
		private final Map<String, double[]> percentiles = new LinkedHashMap<>();

		/**
		 * Specific service-level objective boundaries for meter IDs starting with the
		 * specified name. The longest match wins. Counters will be published for each
		 * specified boundary. Values can be specified as a double or as a Duration value
		 * (for timer meters, defaulting to ms if no unit specified).
		 */
		private final Map<String, ServiceLevelObjectiveBoundary[]> slo = new LinkedHashMap<>();

		/**
		 * Minimum value that meter IDs starting with the specified name are expected to
		 * observe. The longest match wins. Values can be specified as a double or as a
		 * Duration value (for timer meters, defaulting to ms if no unit specified).
		 */
		private final Map<String, String> minimumExpectedValue = new LinkedHashMap<>();

		/**
		 * Maximum value that meter IDs starting with the specified name are expected to
		 * observe. The longest match wins. Values can be specified as a double or as a
		 * Duration value (for timer meters, defaulting to ms if no unit specified).
		 */
		private final Map<String, String> maximumExpectedValue = new LinkedHashMap<>();

		/**
		 * Maximum amount of time that samples for meter IDs starting with the specified
		 * name are accumulated to decaying distribution statistics before they are reset
		 * and rotated. The longest match wins, the key `all` can also be used to
		 * configure all meters.
		 */
		private final Map<String, Duration> expiry = new LinkedHashMap<>();

		/**
		 * Number of histograms for meter IDs starting with the specified name to keep in
		 * the ring buffer. The longest match wins, the key `all` can also be used to
		 * configure all meters.
		 */
		private final Map<String, Integer> bufferLength = new LinkedHashMap<>();

		/**
		 * Returns the percentiles histogram of the distribution.
		 * @return a Map containing the percentiles histogram, where the keys are the
		 * percentiles and the values are booleans indicating if the percentile is present
		 * in the histogram
		 */
		public Map<String, Boolean> getPercentilesHistogram() {
			return this.percentilesHistogram;
		}

		/**
		 * Returns the map of percentiles.
		 * @return the map of percentiles, where the keys are strings representing the
		 * percentile names and the values are arrays of doubles representing the
		 * percentile values
		 */
		public Map<String, double[]> getPercentiles() {
			return this.percentiles;
		}

		/**
		 * Returns the Service Level Objective boundaries.
		 * @return a Map containing the Service Level Objective boundaries
		 */
		public Map<String, ServiceLevelObjectiveBoundary[]> getSlo() {
			return this.slo;
		}

		/**
		 * Returns the minimum expected value as a map of key-value pairs.
		 * @return the minimum expected value as a map
		 */
		public Map<String, String> getMinimumExpectedValue() {
			return this.minimumExpectedValue;
		}

		/**
		 * Returns the maximum expected value.
		 * @return the maximum expected value as a Map of String keys and String values
		 */
		public Map<String, String> getMaximumExpectedValue() {
			return this.maximumExpectedValue;
		}

		/**
		 * Returns the expiry map, which contains the expiration durations for different
		 * items.
		 * @return the expiry map, where the keys are the item names and the values are
		 * the corresponding expiration durations
		 */
		public Map<String, Duration> getExpiry() {
			return this.expiry;
		}

		/**
		 * Returns the buffer length as a map of string keys and integer values.
		 * @return the buffer length as a map of string keys and integer values
		 */
		public Map<String, Integer> getBufferLength() {
			return this.bufferLength;
		}

	}

}
