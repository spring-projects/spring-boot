/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.QueryOptions;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * Configuration properties for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.data.cassandra")
public class CassandraProperties {

	/**
	 * Keyspace name to use.
	 */
	private String keyspaceName;

	/**
	 * Name of the Cassandra cluster.
	 */
	private String clusterName;

	/**
	 * Cluster node addresses.
	 */
	private final List<String> contactPoints = new ArrayList<>(Collections.singleton("localhost"));

	/**
	 * Port of the Cassandra server.
	 */
	private int port = ProtocolOptions.DEFAULT_PORT;

	/**
	 * Login user of the server.
	 */
	private String username;

	/**
	 * Login password of the server.
	 */
	private String password;

	/**
	 * Compression supported by the Cassandra binary protocol.
	 */
	private Compression compression = Compression.NONE;

	/**
	 * Queries consistency level.
	 */
	private ConsistencyLevel consistencyLevel;

	/**
	 * Queries serial consistency level.
	 */
	private ConsistencyLevel serialConsistencyLevel;

	/**
	 * Queries default fetch size.
	 */
	private int fetchSize = QueryOptions.DEFAULT_FETCH_SIZE;

	/**
	 * Socket option: connection time out.
	 */
	private Duration connectTimeout;

	/**
	 * Socket option: read time out.
	 */
	private Duration readTimeout;

	/**
	 * Schema action to take at startup.
	 */
	private String schemaAction = "none";

	/**
	 * Enable SSL support.
	 */
	private boolean ssl = false;

	/**
	 * Whether to enable JMX reporting. Default to false as Cassandra JMX reporting is not
	 * compatible with Dropwizard Metrics.
	 */
	private boolean jmxEnabled;

	/**
	 * Pool configuration.
	 */
	private final Pool pool = new Pool();

	public String getKeyspaceName() {
		return this.keyspaceName;
	}

	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

	public String getClusterName() {
		return this.clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public List<String> getContactPoints() {
		return this.contactPoints;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Compression getCompression() {
		return this.compression;
	}

	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public void setConsistencyLevel(ConsistencyLevel consistency) {
		this.consistencyLevel = consistency;
	}

	public ConsistencyLevel getSerialConsistencyLevel() {
		return this.serialConsistencyLevel;
	}

	public void setSerialConsistencyLevel(ConsistencyLevel serialConsistency) {
		this.serialConsistencyLevel = serialConsistency;
	}

	public int getFetchSize() {
		return this.fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public boolean isSsl() {
		return this.ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public boolean isJmxEnabled() {
		return this.jmxEnabled;
	}

	public void setJmxEnabled(boolean jmxEnabled) {
		this.jmxEnabled = jmxEnabled;
	}

	public String getSchemaAction() {
		return this.schemaAction;
	}

	public void setSchemaAction(String schemaAction) {
		this.schemaAction = schemaAction;
	}

	public Pool getPool() {
		return this.pool;
	}

	/**
	 * Pool properties.
	 */
	public static class Pool {

		/**
		 * Idle timeout before an idle connection is removed. If a duration suffix is not
		 * specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration idleTimeout = Duration.ofSeconds(120);

		/**
		 * Pool timeout when trying to acquire a connection from a host's pool.
		 */
		private Duration poolTimeout = Duration.ofMillis(5000);

		/**
		 * Heartbeat interval after which a message is sent on an idle connection to make
		 * sure it's still alive. If a duration suffix is not specified, seconds will be
		 * used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration heartbeatInterval = Duration.ofSeconds(30);

		/**
		 * Maximum number of requests that get queued if no connection is available.
		 */
		private int maxQueueSize = 256;

		public Duration getIdleTimeout() {
			return this.idleTimeout;
		}

		public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public Duration getPoolTimeout() {
			return this.poolTimeout;
		}

		public void setPoolTimeout(Duration poolTimeout) {
			this.poolTimeout = poolTimeout;
		}

		public Duration getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		public void setHeartbeatInterval(Duration heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

		public int getMaxQueueSize() {
			return this.maxQueueSize;
		}

		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

	}

}
