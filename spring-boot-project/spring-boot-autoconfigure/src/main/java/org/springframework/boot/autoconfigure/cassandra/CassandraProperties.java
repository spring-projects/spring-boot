/*
 * Copyright 2012-2020 the original author or authors.
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

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
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
	 * Name of the Cassandra session.
	 */
	private String sessionName;

	/**
	 * Cluster node addresses in the form 'host:port', or a simple 'host' to use the
	 * configured port.
	 */
	private final List<String> contactPoints = new ArrayList<>(Collections.singleton("127.0.0.1:9042"));

	/**
	 * Port to use if a contact point does not specify one.
	 */
	private int port = 9042;

	/**
	 * Datacenter that is considered "local". Contact points should be from this
	 * datacenter.
	 */
	private String localDatacenter;

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
	private DefaultConsistencyLevel consistencyLevel;

	/**
	 * Queries serial consistency level.
	 */
	private DefaultConsistencyLevel serialConsistencyLevel;

	/**
	 * Queries default page size.
	 */
	private int pageSize = 5000;

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
	 * Pool configuration.
	 */
	private final Pool pool = new Pool();

	public String getKeyspaceName() {
		return this.keyspaceName;
	}

	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

	public String getSessionName() {
		return this.sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.data.cassandra.session-name")
	public String getClusterName() {
		return getSessionName();
	}

	@Deprecated
	public void setClusterName(String clusterName) {
		setSessionName(clusterName);
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

	public String getLocalDatacenter() {
		return this.localDatacenter;
	}

	public void setLocalDatacenter(String localDatacenter) {
		this.localDatacenter = localDatacenter;
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

	public DefaultConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public void setConsistencyLevel(DefaultConsistencyLevel consistency) {
		this.consistencyLevel = consistency;
	}

	public DefaultConsistencyLevel getSerialConsistencyLevel() {
		return this.serialConsistencyLevel;
	}

	public void setSerialConsistencyLevel(DefaultConsistencyLevel serialConsistency) {
		this.serialConsistencyLevel = serialConsistency;
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.data.cassandra.page-size")
	public int getFetchSize() {
		return getPageSize();
	}

	@Deprecated
	public void setFetchSize(int fetchSize) {
		setPageSize(fetchSize);
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
		NONE;

	}

}
