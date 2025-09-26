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

package org.springframework.boot.grpc.server.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import io.grpc.TlsServerCredentials.ClientAuth;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.grpc.internal.GrpcUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(prefix = "spring.grpc.server")
public class GrpcServerProperties {

	/**
	 * Server should listen to any IPv4 and IPv6 address.
	 */
	public static final String ANY_IP_ADDRESS = "*";

	/**
	 * The address to bind to in the form 'host:port' or a pseudo URL like
	 * 'static://host:port'. When the address is set it takes precedence over any
	 * configured host/port values.
	 */
	private @Nullable String address;

	/**
	 * Server host to bind to. The default is any IP address ('*').
	 */
	private String host = ANY_IP_ADDRESS;

	/**
	 * Server port to listen on. When the value is 0, a random available port is selected.
	 */
	private int port = GrpcUtils.DEFAULT_PORT;

	/**
	 * Maximum message size allowed to be received by the server (default 4MiB).
	 */
	@DataSizeUnit(DataUnit.BYTES)
	private DataSize maxInboundMessageSize = DataSize.ofBytes(4194304);

	/**
	 * Maximum metadata size allowed to be received by the server (default 8KiB).
	 */
	@DataSizeUnit(DataUnit.BYTES)
	private DataSize maxInboundMetadataSize = DataSize.ofBytes(8192);

	/**
	 * Maximum time to wait for the server to gracefully shutdown. When the value is
	 * negative, the server waits forever. When the value is 0, the server will force
	 * shutdown immediately. The default is 30 seconds.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration shutdownGracePeriod = Duration.ofSeconds(30);

	private final Health health = new Health();

	private final Inprocess inprocess = new Inprocess();

	private final KeepAlive keepAlive = new KeepAlive();

	private final Ssl ssl = new Ssl();

	public @Nullable String getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable String address) {
		this.address = address;
	}

	/**
	 * Returns the configured address or an address created from the configured host and
	 * port if no address has been set.
	 * @return the address to bind to
	 */
	public String determineAddress() {
		return (this.address != null) ? this.address : this.host + ":" + this.port;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public DataSize getMaxInboundMessageSize() {
		return this.maxInboundMessageSize;
	}

	public void setMaxInboundMessageSize(DataSize maxInboundMessageSize) {
		this.maxInboundMessageSize = maxInboundMessageSize;
	}

	public DataSize getMaxInboundMetadataSize() {
		return this.maxInboundMetadataSize;
	}

	public void setMaxInboundMetadataSize(DataSize maxInboundMetadataSize) {
		this.maxInboundMetadataSize = maxInboundMetadataSize;
	}

	public Duration getShutdownGracePeriod() {
		return this.shutdownGracePeriod;
	}

	public void setShutdownGracePeriod(Duration shutdownGracePeriod) {
		this.shutdownGracePeriod = shutdownGracePeriod;
	}

	public Health getHealth() {
		return this.health;
	}

	public Inprocess getInprocess() {
		return this.inprocess;
	}

	public KeepAlive getKeepAlive() {
		return this.keepAlive;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Health {

		/**
		 * Whether to auto-configure Health feature on the gRPC server.
		 */
		private boolean enabled = true;

		private final Actuator actuator = new Actuator();

		public boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Actuator getActuator() {
			return this.actuator;
		}

	}

	public static class Actuator {

		/**
		 * Whether to adapt Actuator health indicators into gRPC health checks.
		 */
		private boolean enabled = true;

		/**
		 * Whether to update the overall gRPC server health (the '' service) with the
		 * aggregate status of the configured health indicators.
		 */
		private boolean updateOverallHealth = true;

		/**
		 * How often to update the health status.
		 */
		private Duration updateRate = Duration.ofSeconds(5);

		/**
		 * The initial delay before updating the health status the very first time.
		 */
		private Duration updateInitialDelay = Duration.ofSeconds(5);

		/**
		 * List of Actuator health indicator paths to adapt into gRPC health checks.
		 */
		private List<String> healthIndicatorPaths = new ArrayList<>();

		public boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean getUpdateOverallHealth() {
			return this.updateOverallHealth;
		}

		public void setUpdateOverallHealth(boolean updateOverallHealth) {
			this.updateOverallHealth = updateOverallHealth;
		}

		public Duration getUpdateRate() {
			return this.updateRate;
		}

		public void setUpdateRate(Duration updateRate) {
			this.updateRate = updateRate;
		}

		public Duration getUpdateInitialDelay() {
			return this.updateInitialDelay;
		}

		public void setUpdateInitialDelay(Duration updateInitialDelay) {
			this.updateInitialDelay = updateInitialDelay;
		}

		public List<String> getHealthIndicatorPaths() {
			return this.healthIndicatorPaths;
		}

		public void setHealthIndicatorPaths(List<String> healthIndicatorPaths) {
			this.healthIndicatorPaths = healthIndicatorPaths;
		}

	}

	public static class Inprocess {

		/**
		 * The name of the in-process server or null to not start the in-process server.
		 */
		private @Nullable String name;

		public @Nullable String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

	}

	public static class KeepAlive {

		/**
		 * Duration without read activity before sending a keep alive ping (default 2h).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration time = Duration.ofHours(2);

		/**
		 * Maximum time to wait for read activity after sending a keep alive ping. If
		 * sender does not receive an acknowledgment within this time, it will close the
		 * connection (default 20s).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration timeout = Duration.ofSeconds(20);

		/**
		 * Maximum time a connection can remain idle before being gracefully terminated
		 * (default infinite).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration maxIdle;

		/**
		 * Maximum time a connection may exist before being gracefully terminated (default
		 * infinite).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration maxAge;

		/**
		 * Maximum time for graceful connection termination (default infinite).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration maxAgeGrace;

		/**
		 * Maximum keep-alive time clients are permitted to configure (default 5m).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration permitTime = Duration.ofMinutes(5);

		/**
		 * Whether clients are permitted to send keep alive pings when there are no
		 * outstanding RPCs on the connection (default false).
		 */
		private boolean permitWithoutCalls;

		public @Nullable Duration getTime() {
			return this.time;
		}

		public void setTime(@Nullable Duration time) {
			this.time = time;
		}

		public @Nullable Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(@Nullable Duration timeout) {
			this.timeout = timeout;
		}

		public @Nullable Duration getMaxIdle() {
			return this.maxIdle;
		}

		public void setMaxIdle(@Nullable Duration maxIdle) {
			this.maxIdle = maxIdle;
		}

		public @Nullable Duration getMaxAge() {
			return this.maxAge;
		}

		public void setMaxAge(@Nullable Duration maxAge) {
			this.maxAge = maxAge;
		}

		public @Nullable Duration getMaxAgeGrace() {
			return this.maxAgeGrace;
		}

		public void setMaxAgeGrace(@Nullable Duration maxAgeGrace) {
			this.maxAgeGrace = maxAgeGrace;
		}

		public @Nullable Duration getPermitTime() {
			return this.permitTime;
		}

		public void setPermitTime(@Nullable Duration permitTime) {
			this.permitTime = permitTime;
		}

		public boolean isPermitWithoutCalls() {
			return this.permitWithoutCalls;
		}

		public void setPermitWithoutCalls(boolean permitWithoutCalls) {
			this.permitWithoutCalls = permitWithoutCalls;
		}

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support.
		 */
		private @Nullable Boolean enabled;

		/**
		 * Client authentication mode.
		 */
		private ClientAuth clientAuth = ClientAuth.NONE;

		/**
		 * SSL bundle name. Should match a bundle configured in spring.ssl.bundle.
		 */
		private @Nullable String bundle;

		/**
		 * Flag to indicate that client authentication is secure (i.e. certificates are
		 * checked). Do not set this to false in production.
		 */
		private boolean secure = true;

		public @Nullable Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(@Nullable Boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Determine whether to enable SSL support. When the {@code enabled} property is
		 * specified it determines enablement. Otherwise, the support is enabled if the
		 * {@code bundle} is provided.
		 * @return whether to enable SSL support
		 */
		public boolean determineEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

		public void setClientAuth(ClientAuth clientAuth) {
			this.clientAuth = clientAuth;
		}

		public ClientAuth getClientAuth() {
			return this.clientAuth;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public boolean isSecure() {
			return this.secure;
		}

	}

}
