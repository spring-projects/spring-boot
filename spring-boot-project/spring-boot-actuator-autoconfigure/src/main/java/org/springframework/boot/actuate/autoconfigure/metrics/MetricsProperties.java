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
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
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

	public boolean isUseGlobalRegistry() {
		return this.useGlobalRegistry;
	}

	public void setUseGlobalRegistry(boolean useGlobalRegistry) {
		this.useGlobalRegistry = useGlobalRegistry;
	}

	public Map<String, Boolean> getEnable() {
		return this.enable;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	@DeprecatedConfigurationProperty(replacement = "management.observations.key-values", since = "3.2.0")
	public Map<String, String> getTags() {
		return this.tags;
	}

	public Web getWeb() {
		return this.web;
	}

	public Data getData() {
		return this.data;
	}

	public System getSystem() {
		return this.system;
	}

	public Distribution getDistribution() {
		return this.distribution;
	}

	public static class Web {

		private final Client client = new Client();

		private final Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public Server getServer() {
			return this.server;
		}

		public static class Client {

			/**
			 * Maximum number of unique URI tag values allowed. After the max number of
			 * tag values is reached, metrics with additional tag values are denied by
			 * filter.
			 */
			private int maxUriTags = 100;

			public int getMaxUriTags() {
				return this.maxUriTags;
			}

			public void setMaxUriTags(int maxUriTags) {
				this.maxUriTags = maxUriTags;
			}

		}

		public static class Server {

			/**
			 * Maximum number of unique URI tag values allowed. After the max number of
			 * tag values is reached, metrics with additional tag values are denied by
			 * filter.
			 */
			private int maxUriTags = 100;

			public int getMaxUriTags() {
				return this.maxUriTags;
			}

			public void setMaxUriTags(int maxUriTags) {
				this.maxUriTags = maxUriTags;
			}

		}

	}

	public static class Data {

		private final Repository repository = new Repository();

		public Repository getRepository() {
			return this.repository;
		}

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

			public String getMetricName() {
				return this.metricName;
			}

			public void setMetricName(String metricName) {
				this.metricName = metricName;
			}

			public AutoTimeProperties getAutotime() {
				return this.autotime;
			}

		}

	}

	public static class System {

		private final Diskspace diskspace = new Diskspace();

		public Diskspace getDiskspace() {
			return this.diskspace;
		}

		public static class Diskspace {

			/**
			 * Comma-separated list of paths to report disk metrics for.
			 */
			private List<File> paths = new ArrayList<>(Collections.singletonList(new File(".")));

			public List<File> getPaths() {
				return this.paths;
			}

			public void setPaths(List<File> paths) {
				this.paths = paths;
			}

		}

	}

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

		public Map<String, Boolean> getPercentilesHistogram() {
			return this.percentilesHistogram;
		}

		public Map<String, double[]> getPercentiles() {
			return this.percentiles;
		}

		public Map<String, ServiceLevelObjectiveBoundary[]> getSlo() {
			return this.slo;
		}

		public Map<String, String> getMinimumExpectedValue() {
			return this.minimumExpectedValue;
		}

		public Map<String, String> getMaximumExpectedValue() {
			return this.maximumExpectedValue;
		}

		public Map<String, Duration> getExpiry() {
			return this.expiry;
		}

		public Map<String, Integer> getBufferLength() {
			return this.bufferLength;
		}

	}

}
