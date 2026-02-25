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

import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.grpc.TlsServerCredentials.ClientAuth;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * {@link ConfigurationProperties Properties} for Spring gRPC servers.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@ConfigurationProperties("spring.grpc.server")
public class GrpcServerProperties {

	/**
	 * Port on which the gRPC server should listen. Use '0' to bind to a dynamic port.
	 */
	private @Nullable Integer port;

	/**
	 * Network address to which the gRPC server should bind.
	 */
	private @Nullable InetAddress address;

	private final Shutdown shutdown = new Shutdown();

	private final Inbound inbound = new Inbound();

	private final Inprocess inprocess = new Inprocess();

	private final Keepalive keepalive = new Keepalive();

	private final Ssl ssl = new Ssl();

	private final Netty netty = new Netty();

	private final Servlet servlet = new Servlet();

	public @Nullable Integer getPort() {
		return this.port;
	}

	public void setPort(@Nullable Integer port) {
		this.port = port;
	}

	public @Nullable InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable InetAddress address) {
		this.address = address;
	}

	public Shutdown getShutdown() {
		return this.shutdown;
	}

	public Inbound getInbound() {
		return this.inbound;
	}

	public Inprocess getInprocess() {
		return this.inprocess;
	}

	public Keepalive getKeepalive() {
		return this.keepalive;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public Netty getNetty() {
		return this.netty;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

	/**
	 * Server shutdown properties.
	 */
	public static class Shutdown {

		/**
		 * Maximum time to wait for the server to gracefully shutdown. When the value is
		 * negative, the server waits forever. When the value is 0, the server will force
		 * shutdown immediately. The default is 30 seconds.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration gracePeriod = Duration.ofSeconds(30);

		public Duration getGracePeriod() {
			return this.gracePeriod;
		}

		public void setGracePeriod(Duration gracePeriod) {
			this.gracePeriod = gracePeriod;
		}

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
			 * Maximum message size allowed to be received by the server (default 4MiB).
			 */
			@DataSizeUnit(DataUnit.BYTES)
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
			 * Maximum metadata size allowed to be received by the server (default 8KiB).
			 */
			@DataSizeUnit(DataUnit.BYTES)
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
	 * In-process gRPC properties.
	 */
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

	/**
	 * Keep-alive properties.
	 */
	public static class Keepalive {

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

		private final Permit permit = new Permit();

		private final Connection connection = new Connection();

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

		public Permit getPermit() {
			return this.permit;
		}

		public Connection getConnection() {
			return this.connection;
		}

		/**
		 * Keep-alive permit properties.
		 */
		public static class Permit {

			/**
			 * Maximum keep-alive time clients are permitted to configure (default 5m).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration time = Duration.ofMinutes(5);

			/**
			 * Whether clients are permitted to send keep alive pings when there are no
			 * outstanding RPCs on the connection (default false).
			 */
			private boolean withoutCalls;

			public @Nullable Duration getTime() {
				return this.time;
			}

			public void setTime(@Nullable Duration time) {
				this.time = time;
			}

			public boolean isWithoutCalls() {
				return this.withoutCalls;
			}

			public void setWithoutCalls(boolean withoutCalls) {
				this.withoutCalls = withoutCalls;
			}

		}

		/**
		 * Keep-alive connection properties.
		 */
		public static class Connection {

			/**
			 * Maximum time a connection can remain idle before being gracefully
			 * terminated (default infinite).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration maxIdleTime;

			/**
			 * Maximum time a connection may exist before being gracefully terminated
			 * (default infinite).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration maxAge;

			/**
			 * Maximum time for graceful connection termination (default infinite).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration gracePeriod = Duration.ofSeconds(30);

			public @Nullable Duration getMaxIdleTime() {
				return this.maxIdleTime;
			}

			public void setMaxIdleTime(@Nullable Duration maxIdleTime) {
				this.maxIdleTime = maxIdleTime;
			}

			public @Nullable Duration getMaxAge() {
				return this.maxAge;
			}

			public void setMaxAge(@Nullable Duration maxAge) {
				this.maxAge = maxAge;
			}

			public @Nullable Duration getGracePeriod() {
				return this.gracePeriod;
			}

			public void setGracePeriod(@Nullable Duration gracePeriod) {
				this.gracePeriod = gracePeriod;
			}

		}

	}

	/**
	 * SSL properties.
	 */
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

	/**
	 * Netty server properties.
	 */
	public static class Netty {

		/**
		 * Transport mechanism used for Netty and Netty Shaded servers. If not specified
		 * will the appropriate transport will be picked based on the
		 * 'deomain-socket-path' or 'address/port'.
		 */
		private @Nullable Transport transport;

		/**
		 * Path of the domain socket that should be used.
		 */
		private @Nullable String domainSocketPath;

		public @Nullable Transport getTransport() {
			return this.transport;
		}

		public void setTransport(@Nullable Transport transport) {
			this.transport = transport;
		}

		public @Nullable String getDomainSocketPath() {
			return this.domainSocketPath;
		}

		public void setDomainSocketPath(@Nullable String domainSocketPath) {
			this.domainSocketPath = domainSocketPath;
		}

		public enum Transport {

			/**
			 * TCP transport.
			 */
			TCP,

			/**
			 * Domain socket transport.
			 */
			DOMAIN_SOCKET

		}

	}

	/**
	 * Servlet properties.
	 */
	public static class Servlet {

		/**
		 * Whether to use a servlet server in a servlet-based web application. When the
		 * value is false, a native gRPC server will be created as long as one is
		 * available, and it will listen on its own port. Should only be needed if the
		 * GrpcServlet is on the classpath.
		 */
		private boolean enabled;

		/**
		 * Whether to validate that HTTP/2 is enabled. Validation may need to be skipped
		 * if your servlet container is not configured using properties.
		 */
		private boolean validateHttp2 = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isValidateHttp2() {
			return this.validateHttp2;
		}

		public void setValidateHttp2(boolean validateHttp2) {
			this.validateHttp2 = validateHttp2;
		}

	}

}
