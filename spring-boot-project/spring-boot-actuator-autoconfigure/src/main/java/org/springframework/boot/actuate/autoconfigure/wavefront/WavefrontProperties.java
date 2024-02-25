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

import com.wavefront.sdk.common.clients.service.token.TokenService.Type;

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
	 * Type of the API token.
	 */
	private TokenType apiTokenType;

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

	/**
	 * Returns the application associated with this WavefrontProperties object.
	 * @return the application associated with this WavefrontProperties object
	 */
	public Application getApplication() {
		return this.application;
	}

	/**
	 * Returns the sender object associated with this WavefrontProperties instance.
	 * @return the sender object
	 */
	public Sender getSender() {
		return this.sender;
	}

	/**
	 * Returns the metrics object associated with this WavefrontProperties instance.
	 * @return the metrics object
	 */
	public Metrics getMetrics() {
		return this.metrics;
	}

	/**
	 * Returns the URI associated with this WavefrontProperties object.
	 * @return the URI associated with this WavefrontProperties object
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Sets the URI for the WavefrontProperties.
	 * @param uri the URI to be set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * Returns the source of the WavefrontProperties.
	 * @return the source of the WavefrontProperties
	 */
	public String getSource() {
		return this.source;
	}

	/**
	 * Sets the source for the WavefrontProperties.
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Returns the API token.
	 * @return the API token
	 */
	public String getApiToken() {
		return this.apiToken;
	}

	/**
	 * Sets the API token for the WavefrontProperties class.
	 * @param apiToken the API token to be set
	 */
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
		if (this.apiTokenType != TokenType.NO_TOKEN && this.apiToken == null && !usesProxy()) {
			throw new InvalidConfigurationPropertyValueException("management.wavefront.api-token", null,
					"This property is mandatory whenever publishing directly to the Wavefront API");
		}
		return this.apiToken;
	}

	/**
	 * Returns the source value if it is not null, otherwise returns the default source
	 * value.
	 * @return the source value or the default source value
	 */
	public String getSourceOrDefault() {
		if (this.source != null) {
			return this.source;
		}
		return getSourceDefault();
	}

	/**
	 * Returns the default source for the WavefrontProperties class.
	 * @return the default source as a String
	 */
	private String getSourceDefault() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex) {
			return "unknown";
		}
	}

	/**
	 * Returns true if the URI scheme is "proxy", indicating that a proxy is being used.
	 * @return true if a proxy is being used, false otherwise
	 */
	private boolean usesProxy() {
		return "proxy".equals(this.uri.getScheme());
	}

	/**
	 * Returns the set of trace-derived custom tag keys.
	 * @return the set of trace-derived custom tag keys
	 */
	public Set<String> getTraceDerivedCustomTagKeys() {
		return this.traceDerivedCustomTagKeys;
	}

	/**
	 * Sets the trace derived custom tag keys for the WavefrontProperties.
	 * @param traceDerivedCustomTagKeys the set of trace derived custom tag keys to be set
	 */
	public void setTraceDerivedCustomTagKeys(Set<String> traceDerivedCustomTagKeys) {
		this.traceDerivedCustomTagKeys = traceDerivedCustomTagKeys;
	}

	/**
	 * Returns the API token type.
	 * @return the API token type
	 */
	public TokenType getApiTokenType() {
		return this.apiTokenType;
	}

	/**
	 * Sets the API token type for the WavefrontProperties.
	 * @param apiTokenType the API token type to be set
	 */
	public void setApiTokenType(TokenType apiTokenType) {
		this.apiTokenType = apiTokenType;
	}

	/**
	 * Returns the {@link Type Wavefront token type}.
	 * @return the Wavefront token type
	 * @since 3.2.0
	 */
	public Type getWavefrontApiTokenType() {
		if (this.apiTokenType == null) {
			return usesProxy() ? Type.NO_TOKEN : Type.WAVEFRONT_API_TOKEN;
		}
		return switch (this.apiTokenType) {
			case NO_TOKEN -> Type.NO_TOKEN;
			case WAVEFRONT_API_TOKEN -> Type.WAVEFRONT_API_TOKEN;
			case CSP_API_TOKEN -> Type.CSP_API_TOKEN;
			case CSP_CLIENT_CREDENTIALS -> Type.CSP_CLIENT_CREDENTIALS;
		};
	}

	/**
	 * Application class.
	 */
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

		/**
		 * Returns the name of the service.
		 * @return the name of the service
		 */
		public String getServiceName() {
			return this.serviceName;
		}

		/**
		 * Sets the service name for the application.
		 * @param serviceName the name of the service
		 */
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		/**
		 * Returns the name of the Application.
		 * @return the name of the Application
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Sets the name of the application.
		 * @param name the name to be set for the application
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Returns the name of the cluster.
		 * @return the name of the cluster
		 */
		public String getClusterName() {
			return this.clusterName;
		}

		/**
		 * Sets the name of the cluster.
		 * @param clusterName the name of the cluster
		 */
		public void setClusterName(String clusterName) {
			this.clusterName = clusterName;
		}

		/**
		 * Returns the name of the shard.
		 * @return the name of the shard
		 */
		public String getShardName() {
			return this.shardName;
		}

		/**
		 * Sets the name of the shard.
		 * @param shardName the name of the shard to be set
		 */
		public void setShardName(String shardName) {
			this.shardName = shardName;
		}

		/**
		 * Returns the custom tags associated with this Application.
		 * @return a Map containing the custom tags, where the key is the tag name and the
		 * value is the tag value
		 */
		public Map<String, String> getCustomTags() {
			return this.customTags;
		}

		/**
		 * Sets the custom tags for the application.
		 * @param customTags a map containing the custom tags to be set
		 */
		public void setCustomTags(Map<String, String> customTags) {
			this.customTags = customTags;
		}

	}

	/**
	 * Sender class.
	 */
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

		/**
		 * Returns the maximum size of the queue.
		 * @return the maximum size of the queue
		 */
		public int getMaxQueueSize() {
			return this.maxQueueSize;
		}

		/**
		 * Sets the maximum size of the queue.
		 * @param maxQueueSize the maximum size of the queue
		 */
		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		/**
		 * Returns the flush interval of the Sender.
		 * @return the flush interval of the Sender
		 */
		public Duration getFlushInterval() {
			return this.flushInterval;
		}

		/**
		 * Sets the flush interval for the sender.
		 * @param flushInterval the duration between flushes
		 */
		public void setFlushInterval(Duration flushInterval) {
			this.flushInterval = flushInterval;
		}

		/**
		 * Returns the size of the message.
		 * @return the size of the message
		 */
		public DataSize getMessageSize() {
			return this.messageSize;
		}

		/**
		 * Sets the size of the message.
		 * @param messageSize the size of the message
		 */
		public void setMessageSize(DataSize messageSize) {
			this.messageSize = messageSize;
		}

		/**
		 * Returns the batch size used by the Sender.
		 * @return the batch size used by the Sender
		 */
		public int getBatchSize() {
			return this.batchSize;
		}

		/**
		 * Sets the batch size for sending messages.
		 * @param batchSize the batch size to be set
		 */
		public void setBatchSize(int batchSize) {
			this.batchSize = batchSize;
		}

	}

	/**
	 * Metrics class.
	 */
	public static class Metrics {

		/**
		 * Export configuration.
		 */
		private Export export = new Export();

		/**
		 * Returns the export object associated with this Metrics object.
		 * @return the export object
		 */
		public Export getExport() {
			return this.export;
		}

		/**
		 * Sets the export object for the Metrics class.
		 * @param export the export object to be set
		 */
		public void setExport(Export export) {
			this.export = export;
		}

		/**
		 * Export class.
		 */
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

			/**
			 * Returns the global prefix.
			 * @return the global prefix
			 */
			public String getGlobalPrefix() {
				return this.globalPrefix;
			}

			/**
			 * Sets the global prefix for the Export class.
			 * @param globalPrefix the global prefix to be set
			 */
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

			/**
			 * Returns the value of the reportMinuteDistribution property.
			 * @return true if minute distribution is enabled for the report, false
			 * otherwise
			 */
			public boolean isReportMinuteDistribution() {
				return this.reportMinuteDistribution;
			}

			/**
			 * Sets the flag to determine whether to report minute distribution.
			 * @param reportMinuteDistribution true to report minute distribution, false
			 * otherwise
			 */
			public void setReportMinuteDistribution(boolean reportMinuteDistribution) {
				this.reportMinuteDistribution = reportMinuteDistribution;
			}

			/**
			 * Returns the value of the reportHourDistribution property.
			 * @return true if the report hour distribution is enabled, false otherwise
			 */
			public boolean isReportHourDistribution() {
				return this.reportHourDistribution;
			}

			/**
			 * Sets the flag to determine whether to report hour distribution.
			 * @param reportHourDistribution the flag indicating whether to report hour
			 * distribution
			 */
			public void setReportHourDistribution(boolean reportHourDistribution) {
				this.reportHourDistribution = reportHourDistribution;
			}

			/**
			 * Returns a boolean value indicating if the report is based on day
			 * distribution.
			 * @return true if the report is based on day distribution, false otherwise
			 */
			public boolean isReportDayDistribution() {
				return this.reportDayDistribution;
			}

			/**
			 * Sets the flag to determine whether to report day distribution.
			 * @param reportDayDistribution the flag indicating whether to report day
			 * distribution
			 */
			public void setReportDayDistribution(boolean reportDayDistribution) {
				this.reportDayDistribution = reportDayDistribution;
			}

		}

	}

	/**
	 * Wavefront token type.
	 *
	 * @since 3.2.0
	 */
	public enum TokenType {

		/**
		 * No token.
		 */
		NO_TOKEN,
		/**
		 * Wavefront API token.
		 */
		WAVEFRONT_API_TOKEN,
		/**
		 * CSP API token.
		 */
		CSP_API_TOKEN,
		/**
		 * CSP client credentials.
		 */
		CSP_CLIENT_CREDENTIALS

	}

}
