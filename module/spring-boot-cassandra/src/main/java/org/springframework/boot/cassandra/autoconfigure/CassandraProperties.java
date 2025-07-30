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

package org.springframework.boot.cassandra.autoconfigure;

import java.time.Duration;
import java.util.List;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 4.0.0
 */
@ConfigurationProperties("spring.cassandra")
public class CassandraProperties {

	/**
	 * Location of the configuration file to use.
	 */
	private @Nullable Resource config;

	/**
	 * Keyspace name to use.
	 */
	private @Nullable String keyspaceName;

	/**
	 * Name of the Cassandra session.
	 */
	private @Nullable String sessionName;

	/**
	 * Cluster node addresses in the form 'host:port', or a simple 'host' to use the
	 * configured port.
	 */
	private @Nullable List<String> contactPoints;

	/**
	 * Port to use if a contact point does not specify one.
	 */
	private int port = 9042;

	/**
	 * Datacenter that is considered "local". Contact points should be from this
	 * datacenter.
	 */
	private @Nullable String localDatacenter;

	/**
	 * Login user of the server.
	 */
	private @Nullable String username;

	/**
	 * Login password of the server.
	 */
	private @Nullable String password;

	/**
	 * Compression supported by the Cassandra binary protocol.
	 */
	private @Nullable Compression compression;

	/**
	 * Schema action to take at startup.
	 */
	private String schemaAction = "none";

	/**
	 * SSL configuration.
	 */
	private Ssl ssl = new Ssl();

	/**
	 * Connection configuration.
	 */
	private final Connection connection = new Connection();

	/**
	 * Pool configuration.
	 */
	private final Pool pool = new Pool();

	/**
	 * Request configuration.
	 */
	private final Request request = new Request();

	/**
	 * Control connection configuration.
	 */
	private final Controlconnection controlconnection = new Controlconnection();

	public @Nullable Resource getConfig() {
		return this.config;
	}

	public void setConfig(@Nullable Resource config) {
		this.config = config;
	}

	public @Nullable String getKeyspaceName() {
		return this.keyspaceName;
	}

	public void setKeyspaceName(@Nullable String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

	public @Nullable String getSessionName() {
		return this.sessionName;
	}

	public void setSessionName(@Nullable String sessionName) {
		this.sessionName = sessionName;
	}

	public @Nullable List<String> getContactPoints() {
		return this.contactPoints;
	}

	public void setContactPoints(@Nullable List<String> contactPoints) {
		this.contactPoints = contactPoints;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public @Nullable String getLocalDatacenter() {
		return this.localDatacenter;
	}

	public void setLocalDatacenter(@Nullable String localDatacenter) {
		this.localDatacenter = localDatacenter;
	}

	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public @Nullable Compression getCompression() {
		return this.compression;
	}

	public void setCompression(@Nullable Compression compression) {
		this.compression = compression;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public String getSchemaAction() {
		return this.schemaAction;
	}

	public void setSchemaAction(String schemaAction) {
		this.schemaAction = schemaAction;
	}

	public Connection getConnection() {
		return this.connection;
	}

	public Pool getPool() {
		return this.pool;
	}

	public Request getRequest() {
		return this.request;
	}

	public Controlconnection getControlconnection() {
		return this.controlconnection;
	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support.
		 */
		private @Nullable Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

	public static class Connection {

		/**
		 * Timeout to use when establishing driver connections.
		 */
		private @Nullable Duration connectTimeout;

		/**
		 * Timeout to use for internal queries that run as part of the initialization
		 * process, just after a connection is opened.
		 */
		private @Nullable Duration initQueryTimeout;

		public @Nullable Duration getConnectTimeout() {
			return this.connectTimeout;
		}

		public void setConnectTimeout(@Nullable Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public @Nullable Duration getInitQueryTimeout() {
			return this.initQueryTimeout;
		}

		public void setInitQueryTimeout(@Nullable Duration initQueryTimeout) {
			this.initQueryTimeout = initQueryTimeout;
		}

	}

	public static class Request {

		/**
		 * How long the driver waits for a request to complete.
		 */
		private @Nullable Duration timeout;

		/**
		 * Queries consistency level.
		 */
		private @Nullable DefaultConsistencyLevel consistency;

		/**
		 * Queries serial consistency level.
		 */
		private @Nullable DefaultConsistencyLevel serialConsistency;

		/**
		 * How many rows will be retrieved simultaneously in a single network round-trip.
		 */
		private @Nullable Integer pageSize;

		private final Throttler throttler = new Throttler();

		public @Nullable Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(@Nullable Duration timeout) {
			this.timeout = timeout;
		}

		public @Nullable DefaultConsistencyLevel getConsistency() {
			return this.consistency;
		}

		public void setConsistency(@Nullable DefaultConsistencyLevel consistency) {
			this.consistency = consistency;
		}

		public @Nullable DefaultConsistencyLevel getSerialConsistency() {
			return this.serialConsistency;
		}

		public void setSerialConsistency(@Nullable DefaultConsistencyLevel serialConsistency) {
			this.serialConsistency = serialConsistency;
		}

		public @Nullable Integer getPageSize() {
			return this.pageSize;
		}

		public void setPageSize(@Nullable Integer pageSize) {
			this.pageSize = pageSize;
		}

		public Throttler getThrottler() {
			return this.throttler;
		}

	}

	/**
	 * Pool properties.
	 */
	public static class Pool {

		/**
		 * Idle timeout before an idle connection is removed.
		 */
		private @Nullable Duration idleTimeout;

		/**
		 * Heartbeat interval after which a message is sent on an idle connection to make
		 * sure it's still alive.
		 */
		private @Nullable Duration heartbeatInterval;

		public @Nullable Duration getIdleTimeout() {
			return this.idleTimeout;
		}

		public void setIdleTimeout(@Nullable Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public @Nullable Duration getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		public void setHeartbeatInterval(@Nullable Duration heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

	}

	public static class Controlconnection {

		/**
		 * Timeout to use for control queries.
		 */
		private @Nullable Duration timeout;

		public @Nullable Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(@Nullable Duration timeout) {
			this.timeout = timeout;
		}

	}

	public static class Throttler {

		/**
		 * Request throttling type.
		 */
		private @Nullable ThrottlerType type;

		/**
		 * Maximum number of requests that can be enqueued when the throttling threshold
		 * is exceeded.
		 */
		private @Nullable Integer maxQueueSize;

		/**
		 * Maximum number of requests that are allowed to execute in parallel.
		 */
		private @Nullable Integer maxConcurrentRequests;

		/**
		 * Maximum allowed request rate.
		 */
		private @Nullable Integer maxRequestsPerSecond;

		/**
		 * How often the throttler attempts to dequeue requests. Set this high enough that
		 * each attempt will process multiple entries in the queue, but not delay requests
		 * too much.
		 */
		private @Nullable Duration drainInterval;

		public @Nullable ThrottlerType getType() {
			return this.type;
		}

		public void setType(@Nullable ThrottlerType type) {
			this.type = type;
		}

		public @Nullable Integer getMaxQueueSize() {
			return this.maxQueueSize;
		}

		public void setMaxQueueSize(@Nullable Integer maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		public @Nullable Integer getMaxConcurrentRequests() {
			return this.maxConcurrentRequests;
		}

		public void setMaxConcurrentRequests(@Nullable Integer maxConcurrentRequests) {
			this.maxConcurrentRequests = maxConcurrentRequests;
		}

		public @Nullable Integer getMaxRequestsPerSecond() {
			return this.maxRequestsPerSecond;
		}

		public void setMaxRequestsPerSecond(@Nullable Integer maxRequestsPerSecond) {
			this.maxRequestsPerSecond = maxRequestsPerSecond;
		}

		public @Nullable Duration getDrainInterval() {
			return this.drainInterval;
		}

		public void setDrainInterval(@Nullable Duration drainInterval) {
			this.drainInterval = drainInterval;
		}

	}

	/**
	 * Name of the algorithm used to compress protocol frames.
	 */
	public enum Compression {

		/**
		 * Requires 'net.jpountz.lz4:lz4'.
		 */
		LZ4,

		/**
		 * Requires org.xerial.snappy:snappy-java.
		 */
		SNAPPY,

		/**
		 * No compression.
		 */
		NONE

	}

	public enum ThrottlerType {

		/**
		 * Limit the number of requests that can be executed in parallel.
		 */
		CONCURRENCY_LIMITING("ConcurrencyLimitingRequestThrottler"),

		/**
		 * Limits the request rate per second.
		 */
		RATE_LIMITING("RateLimitingRequestThrottler"),

		/**
		 * No request throttling.
		 */
		NONE("PassThroughRequestThrottler");

		private final String type;

		ThrottlerType(String type) {
			this.type = type;
		}

		public String type() {
			return this.type;
		}

	}

}
