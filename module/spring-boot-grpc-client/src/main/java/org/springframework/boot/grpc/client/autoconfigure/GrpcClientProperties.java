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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.grpc.ManagedChannel;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.grpc.client.StubFactory;
import org.springframework.grpc.client.VirtualTargets;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "spring.grpc.client")
public class GrpcClientProperties implements EnvironmentAware, VirtualTargets {

	/**
	 * Map of channels configured by name.
	 */
	private final Map<String, ChannelConfig> channels = new HashMap<>();

	/**
	 * The default channel configuration to use for new channels.
	 */
	private final ChannelConfig defaultChannel = new ChannelConfig();

	/**
	 * Default stub factory to use for all channels.
	 */
	private Class<? extends StubFactory<?>> defaultStubFactory = BlockingStubFactory.class;

	private Environment environment;

	GrpcClientProperties() {
		this.defaultChannel.setAddress("static://localhost:9090");
		this.environment = new StandardEnvironment();
	}

	public Map<String, ChannelConfig> getChannels() {
		return this.channels;
	}

	public ChannelConfig getDefaultChannel() {
		return this.defaultChannel;
	}

	public Class<? extends StubFactory<?>> getDefaultStubFactory() {
		return this.defaultStubFactory;
	}

	public void setDefaultStubFactory(Class<? extends StubFactory<?>> defaultStubFactory) {
		this.defaultStubFactory = defaultStubFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Gets the configured channel with the given name. If no channel is configured for
	 * the specified name then one is created using the default channel as a template.
	 * @param name the name of the channel
	 * @return the configured channel if found, or a newly created channel using the
	 * default channel as a template
	 */
	public ChannelConfig getChannel(String name) {
		if ("default".equals(name)) {
			return this.defaultChannel;
		}
		ChannelConfig channel = this.channels.get(name);
		if (channel != null) {
			return channel;
		}
		channel = this.defaultChannel.copy();
		String address = name;
		if (!name.contains(":/") && !name.startsWith("unix:")) {
			if (name.contains(":")) {
				address = "static://" + name;
			}
			else {
				address = this.defaultChannel.getAddress();
				if (!address.contains(":/")) {
					address = "static://" + address;
				}
			}
		}
		channel.setAddress(address);
		return channel;
	}

	@Override
	public String getTarget(String authority) {
		ChannelConfig channel = this.getChannel(authority);
		String address = channel.getAddress();
		if (address.startsWith("static:") || address.startsWith("tcp:")) {
			address = address.substring(address.indexOf(":") + 1).replaceFirst("/*", "");
		}
		return this.environment.resolvePlaceholders(address);
	}

	/**
	 * Represents the configuration for a {@link ManagedChannel gRPC channel}.
	 */
	public static class ChannelConfig {

		/**
		 * The target address uri to connect to.
		 */
		private String address = "static://localhost:9090";

		/**
		 * The default deadline for RPCs performed on this channel.
		 */
		private @Nullable Duration defaultDeadline;

		/**
		 * The load balancing policy the channel should use.
		 */
		private String defaultLoadBalancingPolicy = "round_robin";

		/**
		 * Whether keep alive is enabled on the channel.
		 */
		private boolean enableKeepAlive;

		private final Health health = new Health();

		/**
		 * The duration without ongoing RPCs before going to idle mode.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration idleTimeout = Duration.ofSeconds(20);

		/**
		 * The delay before sending a keepAlive. Note that shorter intervals increase the
		 * network burden for the server and this value can not be lower than
		 * 'permitKeepAliveTime' on the server.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration keepAliveTime = Duration.ofMinutes(5);

		/**
		 * The default timeout for a keepAlives ping request.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration keepAliveTimeout = Duration.ofSeconds(20);

		/**
		 * Whether a keepAlive will be performed when there are no outstanding RPC on a
		 * connection.
		 */
		private boolean keepAliveWithoutCalls;

		/**
		 * Maximum message size allowed to be received by the channel (default 4MiB). Set
		 * to '-1' to use the highest possible limit (not recommended).
		 */
		private DataSize maxInboundMessageSize = DataSize.ofBytes(4194304);

		/**
		 * Maximum metadata size allowed to be received by the channel (default 8KiB). Set
		 * to '-1' to use the highest possible limit (not recommended).
		 */
		private DataSize maxInboundMetadataSize = DataSize.ofBytes(8192);

		/**
		 * The negotiation type for the channel.
		 */
		private NegotiationType negotiationType = NegotiationType.PLAINTEXT;

		/**
		 * Flag to say that strict SSL checks are not enabled (so the remote certificate
		 * could be anonymous).
		 */
		private boolean secure = true;

		/**
		 * Map representation of the service config to use for the channel.
		 */
		private final Map<String, Object> serviceConfig = new HashMap<>();

		private final Ssl ssl = new Ssl();

		/**
		 * The custom User-Agent for the channel.
		 */
		private @Nullable String userAgent;

		public String getAddress() {
			return this.address;
		}

		public void setAddress(final String address) {
			this.address = address;
		}

		public @Nullable Duration getDefaultDeadline() {
			return this.defaultDeadline;
		}

		public void setDefaultDeadline(@Nullable Duration defaultDeadline) {
			this.defaultDeadline = defaultDeadline;
		}

		public String getDefaultLoadBalancingPolicy() {
			return this.defaultLoadBalancingPolicy;
		}

		public void setDefaultLoadBalancingPolicy(final String defaultLoadBalancingPolicy) {
			this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
		}

		public boolean isEnableKeepAlive() {
			return this.enableKeepAlive;
		}

		public void setEnableKeepAlive(boolean enableKeepAlive) {
			this.enableKeepAlive = enableKeepAlive;
		}

		public Health getHealth() {
			return this.health;
		}

		public Duration getIdleTimeout() {
			return this.idleTimeout;
		}

		public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public Duration getKeepAliveTime() {
			return this.keepAliveTime;
		}

		public void setKeepAliveTime(Duration keepAliveTime) {
			this.keepAliveTime = keepAliveTime;
		}

		public Duration getKeepAliveTimeout() {
			return this.keepAliveTimeout;
		}

		public void setKeepAliveTimeout(Duration keepAliveTimeout) {
			this.keepAliveTimeout = keepAliveTimeout;
		}

		public boolean isKeepAliveWithoutCalls() {
			return this.keepAliveWithoutCalls;
		}

		public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
			this.keepAliveWithoutCalls = keepAliveWithoutCalls;
		}

		public DataSize getMaxInboundMessageSize() {
			return this.maxInboundMessageSize;
		}

		public void setMaxInboundMessageSize(final DataSize maxInboundMessageSize) {
			this.setMaxInboundSize(maxInboundMessageSize, (s) -> this.maxInboundMessageSize = s,
					"maxInboundMessageSize");
		}

		public DataSize getMaxInboundMetadataSize() {
			return this.maxInboundMetadataSize;
		}

		public void setMaxInboundMetadataSize(DataSize maxInboundMetadataSize) {
			this.setMaxInboundSize(maxInboundMetadataSize, (s) -> this.maxInboundMetadataSize = s,
					"maxInboundMetadataSize");
		}

		private void setMaxInboundSize(DataSize maxSize, Consumer<DataSize> setter, String propertyName) {
			if (maxSize != null && maxSize.toBytes() >= 0) {
				setter.accept(maxSize);
			}
			else if (maxSize != null && maxSize.toBytes() == -1) {
				setter.accept(DataSize.ofBytes(Integer.MAX_VALUE));
			}
			else {
				throw new IllegalArgumentException("Unsupported %s: %s".formatted(propertyName, maxSize));
			}
		}

		public NegotiationType getNegotiationType() {
			return this.negotiationType;
		}

		public void setNegotiationType(NegotiationType negotiationType) {
			this.negotiationType = negotiationType;
		}

		public boolean isSecure() {
			return this.secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public Map<String, Object> getServiceConfig() {
			return this.serviceConfig;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public @Nullable String getUserAgent() {
			return this.userAgent;
		}

		public void setUserAgent(@Nullable String userAgent) {
			this.userAgent = userAgent;
		}

		/**
		 * Provide a copy of the channel instance.
		 * @return a copy of the channel instance.
		 */
		ChannelConfig copy() {
			ChannelConfig copy = new ChannelConfig();
			copy.address = this.address;
			copy.defaultLoadBalancingPolicy = this.defaultLoadBalancingPolicy;
			copy.negotiationType = this.negotiationType;
			copy.enableKeepAlive = this.enableKeepAlive;
			copy.idleTimeout = this.idleTimeout;
			copy.keepAliveTime = this.keepAliveTime;
			copy.keepAliveTimeout = this.keepAliveTimeout;
			copy.keepAliveWithoutCalls = this.keepAliveWithoutCalls;
			copy.maxInboundMessageSize = this.maxInboundMessageSize;
			copy.maxInboundMetadataSize = this.maxInboundMetadataSize;
			copy.userAgent = this.userAgent;
			copy.defaultDeadline = this.defaultDeadline;
			copy.health.copyValuesFrom(this.getHealth());
			copy.ssl.copyValuesFrom(this.getSsl());
			copy.serviceConfig.putAll(this.serviceConfig);
			return copy;
		}

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

			/**
			 * Copies the values from another instance.
			 * @param other instance to copy values from
			 */
			void copyValuesFrom(Health other) {
				this.enabled = other.enabled;
				this.serviceName = other.serviceName;
			}

		}

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

			public @Nullable Boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(@Nullable Boolean enabled) {
				this.enabled = enabled;
			}

			public boolean determineEnabled() {
				return (this.enabled != null) ? this.enabled : this.bundle != null;
			}

			public @Nullable String getBundle() {
				return this.bundle;
			}

			public void setBundle(@Nullable String bundle) {
				this.bundle = bundle;
			}

			/**
			 * Copies the values from another instance.
			 * @param other instance to copy values from
			 */
			void copyValuesFrom(Ssl other) {
				this.enabled = other.enabled;
				this.bundle = other.bundle;
			}

		}

	}

}
