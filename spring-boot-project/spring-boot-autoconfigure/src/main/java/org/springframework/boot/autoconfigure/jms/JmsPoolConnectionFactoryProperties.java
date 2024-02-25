/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import java.time.Duration;

/**
 * Configuration properties for connection factory pooling.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class JmsPoolConnectionFactoryProperties {

	/**
	 * Whether a JmsPoolConnectionFactory should be created, instead of a regular
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
	 * Connection idle timeout.
	 */
	private Duration idleTimeout = Duration.ofSeconds(30);

	/**
	 * Maximum number of pooled connections.
	 */
	private int maxConnections = 1;

	/**
	 * Maximum number of pooled sessions per connection in the pool.
	 */
	private int maxSessionsPerConnection = 500;

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

	/**
     * Returns the current status of the enabled flag.
     * 
     * @return true if the enabled flag is set to true, false otherwise
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the JmsPoolConnectionFactoryProperties.
     * 
     * @param enabled the enabled status to be set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns a boolean value indicating whether the connection factory should block if the connection pool is full.
     * 
     * @return true if the connection factory should block if the connection pool is full, false otherwise
     */
    public boolean isBlockIfFull() {
		return this.blockIfFull;
	}

	/**
     * Sets the flag indicating whether to block if the connection pool is full.
     * 
     * @param blockIfFull the flag indicating whether to block if the connection pool is full
     */
    public void setBlockIfFull(boolean blockIfFull) {
		this.blockIfFull = blockIfFull;
	}

	/**
     * Returns the block if full timeout duration.
     * 
     * @return the block if full timeout duration
     */
    public Duration getBlockIfFullTimeout() {
		return this.blockIfFullTimeout;
	}

	/**
     * Sets the timeout duration for blocking if the connection pool is full.
     * 
     * @param blockIfFullTimeout the timeout duration for blocking if the connection pool is full
     */
    public void setBlockIfFullTimeout(Duration blockIfFullTimeout) {
		this.blockIfFullTimeout = blockIfFullTimeout;
	}

	/**
     * Returns the idle timeout duration for the JmsPoolConnectionFactoryProperties.
     *
     * @return the idle timeout duration
     */
    public Duration getIdleTimeout() {
		return this.idleTimeout;
	}

	/**
     * Sets the idle timeout for the JMS pool connection factory.
     * 
     * @param idleTimeout the idle timeout duration to be set
     */
    public void setIdleTimeout(Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	/**
     * Returns the maximum number of connections allowed by this JmsPoolConnectionFactoryProperties.
     *
     * @return the maximum number of connections allowed
     */
    public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
     * Sets the maximum number of connections allowed in the connection pool.
     * 
     * @param maxConnections the maximum number of connections to set
     */
    public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
     * Returns the maximum number of sessions allowed per connection.
     *
     * @return the maximum number of sessions per connection
     */
    public int getMaxSessionsPerConnection() {
		return this.maxSessionsPerConnection;
	}

	/**
     * Sets the maximum number of sessions per connection.
     * 
     * @param maxSessionsPerConnection the maximum number of sessions per connection to set
     */
    public void setMaxSessionsPerConnection(int maxSessionsPerConnection) {
		this.maxSessionsPerConnection = maxSessionsPerConnection;
	}

	/**
     * Returns the time between expiration check.
     * 
     * @return the time between expiration check
     */
    public Duration getTimeBetweenExpirationCheck() {
		return this.timeBetweenExpirationCheck;
	}

	/**
     * Sets the time between expiration check for the JMS pool connection factory.
     * 
     * @param timeBetweenExpirationCheck the duration between expiration checks
     */
    public void setTimeBetweenExpirationCheck(Duration timeBetweenExpirationCheck) {
		this.timeBetweenExpirationCheck = timeBetweenExpirationCheck;
	}

	/**
     * Returns a boolean value indicating whether anonymous producers are used.
     * 
     * @return true if anonymous producers are used, false otherwise
     */
    public boolean isUseAnonymousProducers() {
		return this.useAnonymousProducers;
	}

	/**
     * Sets whether to use anonymous producers.
     * 
     * @param useAnonymousProducers true to use anonymous producers, false otherwise
     */
    public void setUseAnonymousProducers(boolean useAnonymousProducers) {
		this.useAnonymousProducers = useAnonymousProducers;
	}

}
