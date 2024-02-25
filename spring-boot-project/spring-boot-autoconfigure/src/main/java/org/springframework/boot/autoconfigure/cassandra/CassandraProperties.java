/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.List;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;

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
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cassandra")
public class CassandraProperties {

	/**
	 * Location of the configuration file to use.
	 */
	private Resource config;

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
	private List<String> contactPoints;

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
	private Compression compression;

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

	/**
     * Retrieves the configuration resource.
     *
     * @return the configuration resource
     */
    public Resource getConfig() {
		return this.config;
	}

	/**
     * Sets the configuration resource for the CassandraProperties class.
     * 
     * @param config the configuration resource to be set
     */
    public void setConfig(Resource config) {
		this.config = config;
	}

	/**
     * Returns the name of the keyspace.
     *
     * @return the name of the keyspace
     */
    public String getKeyspaceName() {
		return this.keyspaceName;
	}

	/**
     * Sets the name of the keyspace.
     * 
     * @param keyspaceName the name of the keyspace to be set
     */
    public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

	/**
     * Returns the session name.
     *
     * @return the session name
     */
    public String getSessionName() {
		return this.sessionName;
	}

	/**
     * Sets the session name for the CassandraProperties class.
     * 
     * @param sessionName the session name to be set
     */
    public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	/**
     * Returns the list of contact points.
     *
     * @return the list of contact points
     */
    public List<String> getContactPoints() {
		return this.contactPoints;
	}

	/**
     * Sets the contact points for the Cassandra cluster.
     * 
     * @param contactPoints the list of contact points to set
     */
    public void setContactPoints(List<String> contactPoints) {
		this.contactPoints = contactPoints;
	}

	/**
     * Returns the port number.
     *
     * @return the port number
     */
    public int getPort() {
		return this.port;
	}

	/**
     * Sets the port number for the Cassandra connection.
     * 
     * @param port the port number to set
     */
    public void setPort(int port) {
		this.port = port;
	}

	/**
     * Returns the local datacenter.
     *
     * @return the local datacenter
     */
    public String getLocalDatacenter() {
		return this.localDatacenter;
	}

	/**
     * Sets the local datacenter for the Cassandra properties.
     * 
     * @param localDatacenter the local datacenter to be set
     */
    public void setLocalDatacenter(String localDatacenter) {
		this.localDatacenter = localDatacenter;
	}

	/**
     * Returns the username associated with the Cassandra properties.
     *
     * @return the username
     */
    public String getUsername() {
		return this.username;
	}

	/**
     * Sets the username for the Cassandra connection.
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
		this.username = username;
	}

	/**
     * Returns the password for the Cassandra connection.
     *
     * @return the password for the Cassandra connection
     */
    public String getPassword() {
		return this.password;
	}

	/**
     * Sets the password for the Cassandra connection.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the compression type used by the CassandraProperties object.
     * 
     * @return the compression type
     */
    public Compression getCompression() {
		return this.compression;
	}

	/**
     * Sets the compression algorithm to be used for data compression.
     * 
     * @param compression the compression algorithm to be set
     */
    public void setCompression(Compression compression) {
		this.compression = compression;
	}

	/**
     * Returns the SSL configuration for the Cassandra properties.
     *
     * @return the SSL configuration
     */
    public Ssl getSsl() {
		return this.ssl;
	}

	/**
     * Sets the SSL configuration for the Cassandra properties.
     * 
     * @param ssl the SSL configuration to be set
     */
    public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	/**
     * Returns the schema action.
     * 
     * @return the schema action
     */
    public String getSchemaAction() {
		return this.schemaAction;
	}

	/**
     * Sets the schema action for the CassandraProperties.
     * 
     * @param schemaAction the schema action to be set
     */
    public void setSchemaAction(String schemaAction) {
		this.schemaAction = schemaAction;
	}

	/**
     * Returns the connection object.
     * 
     * @return the connection object
     */
    public Connection getConnection() {
		return this.connection;
	}

	/**
     * Returns the pool associated with this CassandraProperties object.
     *
     * @return the pool associated with this CassandraProperties object
     */
    public Pool getPool() {
		return this.pool;
	}

	/**
     * Returns the request object associated with this CassandraProperties instance.
     *
     * @return the request object
     */
    public Request getRequest() {
		return this.request;
	}

	/**
     * Returns the Controlconnection object associated with this CassandraProperties instance.
     *
     * @return the Controlconnection object
     */
    public Controlconnection getControlconnection() {
		return this.controlconnection;
	}

	/**
     * Ssl class.
     */
    public static class Ssl {

		/**
		 * Whether to enable SSL support.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		/**
         * Returns a boolean value indicating whether the SSL is enabled.
         * 
         * @return true if SSL is enabled, false otherwise
         */
        public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		/**
         * Sets the enabled status of the Ssl object.
         * 
         * @param enabled the boolean value indicating whether the Ssl object is enabled or not
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the bundle associated with this Ssl object.
         * 
         * @return the bundle associated with this Ssl object
         */
        public String getBundle() {
			return this.bundle;
		}

		/**
         * Sets the bundle for the Ssl class.
         * 
         * @param bundle the bundle to be set
         */
        public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

	/**
     * Connection class.
     */
    public static class Connection {

		/**
		 * Timeout to use when establishing driver connections.
		 */
		private Duration connectTimeout;

		/**
		 * Timeout to use for internal queries that run as part of the initialization
		 * process, just after a connection is opened.
		 */
		private Duration initQueryTimeout;

		/**
         * Returns the connect timeout duration.
         *
         * @return the connect timeout duration
         */
        public Duration getConnectTimeout() {
			return this.connectTimeout;
		}

		/**
         * Sets the connection timeout for establishing a connection.
         * 
         * @param connectTimeout the duration to wait for a connection to be established
         */
        public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		/**
         * Returns the initial query timeout value.
         * 
         * @return the initial query timeout value
         */
        public Duration getInitQueryTimeout() {
			return this.initQueryTimeout;
		}

		/**
         * Sets the initial query timeout for the connection.
         * 
         * @param initQueryTimeout the duration of the initial query timeout
         */
        public void setInitQueryTimeout(Duration initQueryTimeout) {
			this.initQueryTimeout = initQueryTimeout;
		}

	}

	/**
     * Request class.
     */
    public static class Request {

		/**
		 * How long the driver waits for a request to complete.
		 */
		private Duration timeout;

		/**
		 * Queries consistency level.
		 */
		private DefaultConsistencyLevel consistency;

		/**
		 * Queries serial consistency level.
		 */
		private DefaultConsistencyLevel serialConsistency;

		/**
		 * How many rows will be retrieved simultaneously in a single network round-trip.
		 */
		private Integer pageSize;

		private final Throttler throttler = new Throttler();

		/**
         * Returns the timeout duration for the request.
         *
         * @return the timeout duration
         */
        public Duration getTimeout() {
			return this.timeout;
		}

		/**
         * Sets the timeout for the request.
         * 
         * @param timeout the duration of the timeout
         */
        public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		/**
         * Returns the default consistency level for the request.
         * 
         * @return the default consistency level
         */
        public DefaultConsistencyLevel getConsistency() {
			return this.consistency;
		}

		/**
         * Sets the consistency level for the request.
         * 
         * @param consistency the consistency level to be set
         */
        public void setConsistency(DefaultConsistencyLevel consistency) {
			this.consistency = consistency;
		}

		/**
         * Returns the default consistency level for serial consistency.
         *
         * @return the default serial consistency level
         */
        public DefaultConsistencyLevel getSerialConsistency() {
			return this.serialConsistency;
		}

		/**
         * Sets the serial consistency level for the request.
         * 
         * @param serialConsistency the serial consistency level to be set
         */
        public void setSerialConsistency(DefaultConsistencyLevel serialConsistency) {
			this.serialConsistency = serialConsistency;
		}

		/**
         * Returns the page size.
         *
         * @return the page size
         */
        public Integer getPageSize() {
			return this.pageSize;
		}

		/**
         * Sets the page size for the request.
         * 
         * @param pageSize the page size to be set
         */
        public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		/**
         * Returns the Throttler object associated with this Request.
         *
         * @return the Throttler object associated with this Request
         */
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
		private Duration idleTimeout;

		/**
		 * Heartbeat interval after which a message is sent on an idle connection to make
		 * sure it's still alive.
		 */
		private Duration heartbeatInterval;

		/**
         * Returns the idle timeout duration for the pool.
         *
         * @return the idle timeout duration
         */
        public Duration getIdleTimeout() {
			return this.idleTimeout;
		}

		/**
         * Sets the idle timeout for the pool.
         * 
         * @param idleTimeout the duration of idle time after which an object in the pool is considered idle
         */
        public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		/**
         * Returns the heartbeat interval of the Pool.
         *
         * @return the heartbeat interval of the Pool
         */
        public Duration getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		/**
         * Sets the heartbeat interval for the Pool.
         * 
         * @param heartbeatInterval the duration of the heartbeat interval
         */
        public void setHeartbeatInterval(Duration heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

	}

	/**
     * Controlconnection class.
     */
    public static class Controlconnection {

		/**
		 * Timeout to use for control queries.
		 */
		private Duration timeout;

		/**
         * Returns the timeout duration for the control connection.
         *
         * @return the timeout duration for the control connection
         */
        public Duration getTimeout() {
			return this.timeout;
		}

		/**
         * Sets the timeout for the control connection.
         * 
         * @param timeout the duration of the timeout
         */
        public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	/**
     * Throttler class.
     */
    public static class Throttler {

		/**
		 * Request throttling type.
		 */
		private ThrottlerType type;

		/**
		 * Maximum number of requests that can be enqueued when the throttling threshold
		 * is exceeded.
		 */
		private Integer maxQueueSize;

		/**
		 * Maximum number of requests that are allowed to execute in parallel.
		 */
		private Integer maxConcurrentRequests;

		/**
		 * Maximum allowed request rate.
		 */
		private Integer maxRequestsPerSecond;

		/**
		 * How often the throttler attempts to dequeue requests. Set this high enough that
		 * each attempt will process multiple entries in the queue, but not delay requests
		 * too much.
		 */
		private Duration drainInterval;

		/**
         * Returns the type of the Throttler.
         *
         * @return the type of the Throttler
         */
        public ThrottlerType getType() {
			return this.type;
		}

		/**
         * Sets the type of the throttler.
         * 
         * @param type the type of the throttler
         */
        public void setType(ThrottlerType type) {
			this.type = type;
		}

		/**
         * Returns the maximum size of the queue.
         *
         * @return the maximum size of the queue
         */
        public Integer getMaxQueueSize() {
			return this.maxQueueSize;
		}

		/**
         * Sets the maximum size of the queue.
         * 
         * @param maxQueueSize the maximum size of the queue
         */
        public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		/**
         * Returns the maximum number of concurrent requests allowed by the Throttler.
         *
         * @return the maximum number of concurrent requests
         */
        public Integer getMaxConcurrentRequests() {
			return this.maxConcurrentRequests;
		}

		/**
         * Sets the maximum number of concurrent requests allowed by the throttler.
         * 
         * @param maxConcurrentRequests the maximum number of concurrent requests
         */
        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
			this.maxConcurrentRequests = maxConcurrentRequests;
		}

		/**
         * Returns the maximum number of requests allowed per second.
         *
         * @return the maximum number of requests per second
         */
        public Integer getMaxRequestsPerSecond() {
			return this.maxRequestsPerSecond;
		}

		/**
         * Sets the maximum number of requests allowed per second.
         * 
         * @param maxRequestsPerSecond the maximum number of requests per second
         */
        public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
			this.maxRequestsPerSecond = maxRequestsPerSecond;
		}

		/**
         * Returns the drain interval of the Throttler.
         *
         * @return the drain interval of the Throttler
         */
        public Duration getDrainInterval() {
			return this.drainInterval;
		}

		/**
         * Sets the drain interval for the throttler.
         * 
         * @param drainInterval the duration representing the drain interval
         */
        public void setDrainInterval(Duration drainInterval) {
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

		/**
     * Sets the throttler type for the Cassandra properties.
     *
     * @param type the throttler type to be set
     */
    ThrottlerType(String type) {
			this.type = type;
		}

		/**
     * Returns the type of the CassandraProperties object.
     *
     * @return the type of the CassandraProperties object
     */
    public String type() {
			return this.type;
		}

	}

}
