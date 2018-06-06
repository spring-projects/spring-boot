/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jms.activemq;

import java.time.Duration;

/**
 * Configuration properties for connection factory pooling.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class PooledConnectionFactoryProperties {

	/**
	 * Whether a PooledConnectionFactory should be created, instead of a regular
	 * ConnectionFactory.
	 */
	private boolean enabled;

	/**
	 * Whether to block when a connection is requested and the pool is full. Set it to
	 * false to throw a "JMSException" instead.
	 */
	private boolean blockIfFull = true;

	/**
	 * Blocking period before throwing an exception if the pool is still full.
	 */
	private Duration blockIfFullTimeout = Duration.ofMillis(-1);

	/**
	 * Whether to create a connection on startup. Can be used to warm up the pool on
	 * startup.
	 */
	private boolean createConnectionOnStartup = true;

	/**
	 * Connection expiration timeout.
	 */
	private Duration expiryTimeout = Duration.ofMillis(0);

	/**
	 * Connection idle timeout.
	 */
	private Duration idleTimeout = Duration.ofSeconds(30);

	/**
	 * Maximum number of pooled connections.
	 */
	private int maxConnections = 1;

	/**
	 * Maximum number of active sessions per connection.
	 */
	private int maximumActiveSessionPerConnection = 500;

	/**
	 * Reset the connection when a "JMSException" occurs.
	 */
	private boolean reconnectOnException = true;

	/**
	 * Time to sleep between runs of the idle connection eviction thread. When negative,
	 * no idle connection eviction thread runs.
	 */
	private Duration timeBetweenExpirationCheck = Duration.ofMillis(-1);

	/**
	 * Whether to use only one anonymous "MessageProducer" instance. Set it to false to
	 * create one "MessageProducer" every time one is required.
	 */
	private boolean useAnonymousProducers = true;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isBlockIfFull() {
		return this.blockIfFull;
	}

	public void setBlockIfFull(boolean blockIfFull) {
		this.blockIfFull = blockIfFull;
	}

	public Duration getBlockIfFullTimeout() {
		return this.blockIfFullTimeout;
	}

	public void setBlockIfFullTimeout(Duration blockIfFullTimeout) {
		this.blockIfFullTimeout = blockIfFullTimeout;
	}

	public boolean isCreateConnectionOnStartup() {
		return this.createConnectionOnStartup;
	}

	public void setCreateConnectionOnStartup(boolean createConnectionOnStartup) {
		this.createConnectionOnStartup = createConnectionOnStartup;
	}

	public Duration getExpiryTimeout() {
		return this.expiryTimeout;
	}

	public void setExpiryTimeout(Duration expiryTimeout) {
		this.expiryTimeout = expiryTimeout;
	}

	public Duration getIdleTimeout() {
		return this.idleTimeout;
	}

	public void setIdleTimeout(Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMaximumActiveSessionPerConnection() {
		return this.maximumActiveSessionPerConnection;
	}

	public void setMaximumActiveSessionPerConnection(
			int maximumActiveSessionPerConnection) {
		this.maximumActiveSessionPerConnection = maximumActiveSessionPerConnection;
	}

	public boolean isReconnectOnException() {
		return this.reconnectOnException;
	}

	public void setReconnectOnException(boolean reconnectOnException) {
		this.reconnectOnException = reconnectOnException;
	}

	public Duration getTimeBetweenExpirationCheck() {
		return this.timeBetweenExpirationCheck;
	}

	public void setTimeBetweenExpirationCheck(Duration timeBetweenExpirationCheck) {
		this.timeBetweenExpirationCheck = timeBetweenExpirationCheck;
	}

	public boolean isUseAnonymousProducers() {
		return this.useAnonymousProducers;
	}

	public void setUseAnonymousProducers(boolean useAnonymousProducers) {
		this.useAnonymousProducers = useAnonymousProducers;
	}

}
