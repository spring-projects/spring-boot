/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for gRPC clients.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@ConfigurationProperties("spring.grpc.client")
public class GrpcClientProperties {

	/**
	 * Map of channels configured by name.
	 */
	private final Map<String, Channel> channel = new LinkedHashMap<>();

	public Map<String, Channel> getChannel() {
		return this.channel;
	}

	/**
	 * Channel Properties.
	 */
	public static class Channel {

		static final String DEFAULT_TARGET = "static://localhost:9090";

		/**
		 * Channel target address.
		 */
		private String target = DEFAULT_TARGET;

		/**
		 * Custom User-Agent for the channel.
		 */
		private @Nullable String userAgent;

		/**
		 * Whether to bypass certificate validation for easier testing (so the remote
		 * certificate could be anonymous). Should not be enabled in production.
		 */
		private boolean bypassCertificateValidation;

		/**
		 * Service config for the channel.
		 */
		private @Nullable ServiceConfig serviceConfig;

		private final Inbound inbound = new Inbound();

		@Name("default")
		private final Default defaultProperties = new Default();

		private final Idle idle = new Idle();

		private final Keepalive keepalive = new Keepalive();

		private final Ssl ssl = new Ssl();

		private final Health health = new Health();

		public String getTarget() {
			return this.target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public @Nullable String getUserAgent() {
			return this.userAgent;
		}

		public void setUserAgent(@Nullable String userAgent) {
			this.userAgent = userAgent;
		}

		public boolean isBypassCertificateValidation() {
			return this.bypassCertificateValidation;
		}

		public void setBypassCertificateValidation(boolean bypassCertificateValidation) {
			this.bypassCertificateValidation = bypassCertificateValidation;
		}

		public @Nullable ServiceConfig getServiceConfig() {
			return this.serviceConfig;
		}

		public void setServiceConfig(@Nullable ServiceConfig serviceConfig) {
			this.serviceConfig = serviceConfig;
		}

		public Inbound getInbound() {
			return this.inbound;
		}

		public Default getDefault() {
			return this.defaultProperties;
		}

		public Idle getIdle() {
			return this.idle;
		}

		public Keepalive getKeepalive() {
			return this.keepalive;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public Health getHealth() {
			return this.health;
		}

		/**
		 * In-bound properties.
		 */
		public static class Inbound {

			private final Message message = new Message();

			private final Metadata metadata = new Metadata();

			public Message getMessage() {
				return this.message;
			}

			public Metadata getMetadata() {
				return this.metadata;
			}

			/**
			 * In-bound message properties.
			 */
			public static class Message {

				/**
				 * Maximum message size allowed to be received by the channel. Set to '-1'
				 * to use the highest possible limit (not recommended).
				 */
				private DataSize maxSize = DataSize.ofBytes(4194304);

				public DataSize getMaxSize() {
					return this.maxSize;
				}

				public void setMaxSize(DataSize maxSize) {
					this.maxSize = maxSize;
				}

			}

			/**
			 * In-bound metadata properties.
			 */
			public static class Metadata {

				/**
				 * Maximum metadata size allowed to be received by the channel. Set to
				 * '-1' to use the highest possible limit (not recommended).
				 */
				private DataSize maxSize = DataSize.ofBytes(8192);

				public DataSize getMaxSize() {
					return this.maxSize;
				}

				public void setMaxSize(DataSize maxSize) {
					this.maxSize = maxSize;
				}

			}

		}

		/**
		 * Properties for client defaults.
		 */
		public static class Default {

			/**
			 * Default deadline for RPCs performed on this channel. If a duration suffix
			 * is not specified, seconds will be used.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration deadline;

			/**
			 * Load balancing policy the channel should use.
			 */
			private String loadBalancingPolicy = "round_robin";

			public @Nullable Duration getDeadline() {
				return this.deadline;
			}

			public void setDeadline(@Nullable Duration deadline) {
				this.deadline = deadline;
			}

			public String getLoadBalancingPolicy() {
				return this.loadBalancingPolicy;
			}

			public void setLoadBalancingPolicy(String loadBalancingPolicy) {
				this.loadBalancingPolicy = loadBalancingPolicy;
			}

		}

		/**
		 * Idle properties.
		 */
		public static class Idle {

			/**
			 * Time without ongoing RPCs before going to idle mode. If a duration suffix
			 * is not specified, seconds will be used.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration timeout = Duration.ofSeconds(20);

			public Duration getTimeout() {
				return this.timeout;
			}

			public void setTimeout(Duration timeout) {
				this.timeout = timeout;
			}

		}

		/**
		 * Keep-alive properties.
		 */
		public static class Keepalive {

			/**
			 * Delay before sending a keepAlive. Note that shorter intervals increase the
			 * network burden for the server, and this value cannot be lower than
			 * 'permitKeepAliveTime' on the server. If a duration suffix is not specified,
			 * seconds will be used.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration time = Duration.ofMinutes(5);

			/**
			 * Default timeout for a keepAlives ping request. If a duration suffix is not
			 * specified, seconds will be used.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration timeout = Duration.ofSeconds(20);

			/**
			 * Whether a keepAlive will be performed when there are no outstanding RPC on
			 * a connection.
			 */
			private boolean withoutCalls;

			public Duration getTime() {
				return this.time;
			}

			public void setTime(Duration time) {
				this.time = time;
			}

			public Duration getTimeout() {
				return this.timeout;
			}

			public void setTimeout(Duration timeout) {
				this.timeout = timeout;
			}

			public boolean isWithoutCalls() {
				return this.withoutCalls;
			}

			public void setWithoutCalls(boolean withoutCalls) {
				this.withoutCalls = withoutCalls;
			}

		}

		/**
		 * Health properties.
		 */
		public static class Health {

			/**
			 * Whether to enable client-side health check for the channel.
			 */
			private boolean enabled;

			/**
			 * Name of the service to check health on.
			 */
			private @Nullable String serviceName;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public @Nullable String getServiceName() {
				return this.serviceName;
			}

			public void setServiceName(String serviceName) {
				this.serviceName = serviceName;
			}

		}

		/**
		 * SSL properties.
		 */
		public static class Ssl {

			/**
			 * Whether to enable SSL support. Enabled automatically if "bundle" is
			 * provided unless specified otherwise.
			 */
			private @Nullable Boolean enabled;

			/**
			 * SSL bundle name.
			 */
			private @Nullable String bundle;

			public @Nullable Boolean getEnabled() {
				return this.enabled;
			}

			public void setEnabled(@Nullable Boolean enabled) {
				this.enabled = enabled;
			}

			public @Nullable String getBundle() {
				return this.bundle;
			}

			public void setBundle(@Nullable String bundle) {
				this.bundle = bundle;
			}

		}

	}

}
