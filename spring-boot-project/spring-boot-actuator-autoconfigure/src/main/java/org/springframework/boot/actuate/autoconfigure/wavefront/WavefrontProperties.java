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

package org.springframework.boot.actuate.autoconfigure.wavefront;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PushRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties to configure Wavefront.
 *
 * @author Moritz Halbritter
 * @author Glenn Oppegard
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = "management.wavefront")
public class WavefrontProperties {

	/**
	 * URI to ship metrics and traces to.
	 */
	private URI uri = URI.create("https://longboard.wavefront.com");

	/**
	 * Unique identifier for the app instance that is the source of metrics and traces
	 * being published to Wavefront. Defaults to the local host name.
	 */
	private String source;

	/**
	 * API token used when publishing metrics and traces directly to the Wavefront API
	 * host.
	 */
	private String apiToken;

	/**
	 * Application configuration.
	 */
	private final Application application = new Application();

	/**
	 * Sender configuration.
	 */
	private final Sender sender = new Sender();

	/**
	 * Metrics configuration.
	 */
	private final Metrics metrics = new Metrics();

	/**
	 * Customized span tags for RED metrics.
	 */
	private Set<String> traceDerivedCustomTagKeys = new HashSet<>();

	public Application getApplication() {
		return this.application;
	}

	public Sender getSender() {
		return this.sender;
	}

	public Metrics getMetrics() {
		return this.metrics;
	}

	public URI getUri() {
		return this.uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getApiToken() {
		return this.apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	/**
	 * Returns the effective URI of the wavefront instance. This will not be the same URI
	 * given through {@link #setUri(URI)} when a proxy is used.
	 * @return the effective URI of the wavefront instance
	 */
	public URI getEffectiveUri() {
		if (usesProxy()) {
			// See io.micrometer.wavefront.WavefrontMeterRegistry.getWavefrontReportingUri
			return URI.create(this.uri.toString().replace("proxy://", "http://"));
		}
		return this.uri;
	}

	/**
	 * Returns the API token or throws an exception if the API token is mandatory. If a
	 * proxy is used, the API token is optional.
	 * @return the API token
	 */
	public String getApiTokenOrThrow() {
		if (this.apiToken == null && !usesProxy()) {
			throw new InvalidConfigurationPropertyValueException("management.wavefront.api-token", null,
					"This property is mandatory whenever publishing directly to the Wavefront API");
		}
		return this.apiToken;
	}

	public String getSourceOrDefault() {
		if (this.source != null) {
			return this.source;
		}
		return getSourceDefault();
	}

	private String getSourceDefault() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex) {
			return "unknown";
		}
	}

	private boolean usesProxy() {
		return "proxy".equals(this.uri.getScheme());
	}

	public Set<String> getTraceDerivedCustomTagKeys() {
		return this.traceDerivedCustomTagKeys;
	}

	public void setTraceDerivedCustomTagKeys(Set<String> traceDerivedCustomTagKeys) {
		this.traceDerivedCustomTagKeys = traceDerivedCustomTagKeys;
	}

	public static class Application {

		/**
		 * Wavefront 'Application' name used in ApplicationTags.
		 */
		private String name = "unnamed_application";

		/**
		 * Wavefront 'Service' name used in ApplicationTags, falling back to
		 * 'spring.application.name'. If both are unset it defaults to 'unnamed_service'.
		 */
		private String serviceName;

		/**
		 * Wavefront Cluster name used in ApplicationTags.
		 */
		private String clusterName;

		/**
		 * Wavefront Shard name used in ApplicationTags.
		 */
		private String shardName;

		/**
		 * Wavefront custom tags used in ApplicationTags.
		 */
		private Map<String, String> customTags = new HashMap<>();

		public String getServiceName() {
			return this.serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getClusterName() {
			return this.clusterName;
		}

		public void setClusterName(String clusterName) {
			this.clusterName = clusterName;
		}

		public String getShardName() {
			return this.shardName;
		}

		public void setShardName(String shardName) {
			this.shardName = shardName;
		}

		public Map<String, String> getCustomTags() {
			return this.customTags;
		}

		public void setCustomTags(Map<String, String> customTags) {
			this.customTags = customTags;
		}

	}

	public static class Sender {

		/**
		 * Maximum size of queued messages.
		 */
		private int maxQueueSize = 50000;

		/**
		 * Flush interval to send queued messages.
		 */
		private Duration flushInterval = Duration.ofSeconds(1);

		/**
		 * Maximum size of a message.
		 */
		private DataSize messageSize = DataSize.ofBytes(Integer.MAX_VALUE);

		/**
		 * Number of measurements per request to use for Wavefront. If more measurements
		 * are found, then multiple requests will be made.
		 */
		private int batchSize = 10000;

		public int getMaxQueueSize() {
			return this.maxQueueSize;
		}

		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		public Duration getFlushInterval() {
			return this.flushInterval;
		}

		public void setFlushInterval(Duration flushInterval) {
			this.flushInterval = flushInterval;
		}

		public DataSize getMessageSize() {
			return this.messageSize;
		}

		public void setMessageSize(DataSize messageSize) {
			this.messageSize = messageSize;
		}

		public int getBatchSize() {
			return this.batchSize;
		}

		public void setBatchSize(int batchSize) {
			this.batchSize = batchSize;
		}

	}

	public static class Metrics {

		/**
		 * Export configuration.
		 */
		private Export export = new Export();

		public Export getExport() {
			return this.export;
		}

		public void setExport(Export export) {
			this.export = export;
		}

		public static class Export extends PushRegistryProperties {

			/**
			 * Global prefix to separate metrics originating from this app's
			 * instrumentation from those originating from other Wavefront integrations
			 * when viewed in the Wavefront UI.
			 */
			private String globalPrefix;

			/**
			 * Whether to report histogram distributions aggregated into minute intervals.
			 */
			private boolean reportMinuteDistribution = true;

			/**
			 * Whether to report histogram distributions aggregated into hour intervals.
			 */
			private boolean reportHourDistribution;

			/**
			 * Whether to report histogram distributions aggregated into day intervals.
			 */
			private boolean reportDayDistribution;

			public String getGlobalPrefix() {
				return this.globalPrefix;
			}

			public void setGlobalPrefix(String globalPrefix) {
				this.globalPrefix = globalPrefix;
			}

			/**
			 * See {@link PushRegistryProperties#getBatchSize()}.
			 */
			@Override
			public Integer getBatchSize() {
				throw new UnsupportedOperationException("Use Sender.getBatchSize() instead");
			}

			/**
			 * See {@link PushRegistryProperties#setBatchSize(Integer)}.
			 */
			@Override
			public void setBatchSize(Integer batchSize) {
				throw new UnsupportedOperationException("Use Sender.setBatchSize(int) instead");
			}

			public boolean isReportMinuteDistribution() {
				return this.reportMinuteDistribution;
			}

			public void setReportMinuteDistribution(boolean reportMinuteDistribution) {
				this.reportMinuteDistribution = reportMinuteDistribution;
			}

			public boolean isReportHourDistribution() {
				return this.reportHourDistribution;
			}

			public void setReportHourDistribution(boolean reportHourDistribution) {
				this.reportHourDistribution = reportHourDistribution;
			}

			public boolean isReportDayDistribution() {
				return this.reportDayDistribution;
			}

			public void setReportDayDistribution(boolean reportDayDistribution) {
				this.reportDayDistribution = reportDayDistribution;
			}

		}

	}

}
