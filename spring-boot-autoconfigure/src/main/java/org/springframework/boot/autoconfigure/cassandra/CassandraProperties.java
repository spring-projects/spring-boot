/*
 * Copyright 2012-2016 the original author or authors.
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

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Mark Paluch
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
	 * Comma-separated list of cluster node addresses.
	 */
	private String contactPoints = "localhost";

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
	 * Class name of the load balancing policy. The class must have a default constructor.
	 */
	private Class<? extends LoadBalancingPolicy> loadBalancingPolicy;

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
	 * Class name of the reconnection policy. The class must have a default constructor.
	 */
	private Class<? extends ReconnectionPolicy> reconnectionPolicy;

	/**
	 * Class name of the retry policy. The class must have a default constructor.
	 */
	private Class<? extends RetryPolicy> retryPolicy;

	/**
	 * Socket option: connection time out.
	 */
	private int connectTimeoutMillis = SocketOptions.DEFAULT_CONNECT_TIMEOUT_MILLIS;

	/**
	 * Socket option: read time out.
	 */
	private int readTimeoutMillis = SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS;

	/**
	 * Schema action to take at startup.
	 */
	private String schemaAction = "none";

	/**
	 * Enable SSL support.
	 */
	private boolean ssl = false;

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

	public String getContactPoints() {
		return this.contactPoints;
	}

	public void setContactPoints(String contactPoints) {
		this.contactPoints = contactPoints;
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

	public Class<? extends LoadBalancingPolicy> getLoadBalancingPolicy() {
		return this.loadBalancingPolicy;
	}

	public void setLoadBalancingPolicy(
			Class<? extends LoadBalancingPolicy> loadBalancingPolicy) {
		this.loadBalancingPolicy = loadBalancingPolicy;
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

	public Class<? extends ReconnectionPolicy> getReconnectionPolicy() {
		return this.reconnectionPolicy;
	}

	public void setReconnectionPolicy(
			Class<? extends ReconnectionPolicy> reconnectionPolicy) {
		this.reconnectionPolicy = reconnectionPolicy;
	}

	public Class<? extends RetryPolicy> getRetryPolicy() {
		return this.retryPolicy;
	}

	public void setRetryPolicy(Class<? extends RetryPolicy> retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	public int getConnectTimeoutMillis() {
		return this.connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	public int getReadTimeoutMillis() {
		return this.readTimeoutMillis;
	}

	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
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

}
