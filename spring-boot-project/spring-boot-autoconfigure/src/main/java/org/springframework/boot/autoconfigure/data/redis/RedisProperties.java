/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.redis;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Redis.
 *
 * @author Dave Syer
 * @author Christoph Strobl
 * @author Eddú Meléndez
 * @author Marco Aust
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

	/**
	 * Database index used by the connection factory.
	 */
	private int database = 0;

	/**
	 * Connection URL. Overrides host, port, username, and password. Example:
	 * redis://user:password@example.com:6379
	 */
	private String url;

	/**
	 * Redis server host.
	 */
	private String host = "localhost";

	/**
	 * Login username of the redis server.
	 */
	private String username;

	/**
	 * Login password of the redis server.
	 */
	private String password;

	/**
	 * Redis server port.
	 */
	private int port = 6379;

	/**
	 * Read timeout.
	 */
	private Duration timeout;

	/**
	 * Connection timeout.
	 */
	private Duration connectTimeout;

	/**
	 * Client name to be set on connections with CLIENT SETNAME.
	 */
	private String clientName;

	/**
	 * Type of client to use. By default, auto-detected according to the classpath.
	 */
	private ClientType clientType;

	private Sentinel sentinel;

	private Cluster cluster;

	private final Ssl ssl = new Ssl();

	private final Jedis jedis = new Jedis();

	private final Lettuce lettuce = new Lettuce();

	/**
	 * Returns the value of the database property.
	 * @return the value of the database property
	 */
	public int getDatabase() {
		return this.database;
	}

	/**
	 * Sets the database number for Redis connection.
	 * @param database the database number to be set
	 */
	public void setDatabase(int database) {
		this.database = database;
	}

	/**
	 * Returns the URL of the Redis server.
	 * @return the URL of the Redis server
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Sets the URL for the Redis connection.
	 * @param url the URL to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Returns the host of the Redis server.
	 * @return the host of the Redis server
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host for the Redis connection.
	 * @param host the host address to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns the username associated with the Redis properties.
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username for the Redis connection.
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the password associated with the Redis connection.
	 * @return the password associated with the Redis connection
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for Redis connection.
	 * @param password the password to be set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the port number.
	 * @return the port number
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Sets the port number for the Redis connection.
	 * @param port the port number to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Returns the SSL configuration for Redis connection.
	 * @return the SSL configuration for Redis connection
	 */
	public Ssl getSsl() {
		return this.ssl;
	}

	/**
	 * Sets the timeout for Redis operations.
	 * @param timeout the duration of the timeout
	 */
	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	/**
	 * Returns the timeout duration for Redis operations.
	 * @return the timeout duration
	 */
	public Duration getTimeout() {
		return this.timeout;
	}

	/**
	 * Returns the connect timeout duration.
	 * @return the connect timeout duration
	 */
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	/**
	 * Sets the connection timeout for Redis.
	 * @param connectTimeout the connection timeout duration
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Returns the name of the client.
	 * @return the name of the client
	 */
	public String getClientName() {
		return this.clientName;
	}

	/**
	 * Sets the client name for the RedisProperties.
	 * @param clientName the client name to be set
	 */
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	/**
	 * Returns the client type of the RedisProperties object.
	 * @return the client type of the RedisProperties object
	 */
	public ClientType getClientType() {
		return this.clientType;
	}

	/**
	 * Sets the client type for Redis connection.
	 * @param clientType the client type to be set
	 */
	public void setClientType(ClientType clientType) {
		this.clientType = clientType;
	}

	/**
	 * Returns the sentinel instance associated with this RedisProperties object.
	 * @return the sentinel instance
	 */
	public Sentinel getSentinel() {
		return this.sentinel;
	}

	/**
	 * Sets the sentinel for the RedisProperties.
	 * @param sentinel the sentinel to be set
	 */
	public void setSentinel(Sentinel sentinel) {
		this.sentinel = sentinel;
	}

	/**
	 * Returns the cluster associated with this RedisProperties object.
	 * @return the cluster associated with this RedisProperties object
	 */
	public Cluster getCluster() {
		return this.cluster;
	}

	/**
	 * Sets the cluster for the RedisProperties.
	 * @param cluster the cluster to be set
	 */
	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}

	/**
	 * Returns the Jedis instance.
	 * @return the Jedis instance
	 */
	public Jedis getJedis() {
		return this.jedis;
	}

	/**
	 * Returns the lettuce instance.
	 * @return the lettuce instance
	 */
	public Lettuce getLettuce() {
		return this.lettuce;
	}

	/**
	 * Type of Redis client to use.
	 */
	public enum ClientType {

		/**
		 * Use the Lettuce redis client.
		 */
		LETTUCE,

		/**
		 * Use the Jedis redis client.
		 */
		JEDIS

	}

	/**
	 * Pool properties.
	 */
	public static class Pool {

		/**
		 * Whether to enable the pool. Enabled automatically if "commons-pool2" is
		 * available. With Jedis, pooling is implicitly enabled in sentinel mode and this
		 * setting only applies to single node setup.
		 */
		private Boolean enabled;

		/**
		 * Maximum number of "idle" connections in the pool. Use a negative value to
		 * indicate an unlimited number of idle connections.
		 */
		private int maxIdle = 8;

		/**
		 * Target for the minimum number of idle connections to maintain in the pool. This
		 * setting only has an effect if both it and time between eviction runs are
		 * positive.
		 */
		private int minIdle = 0;

		/**
		 * Maximum number of connections that can be allocated by the pool at a given
		 * time. Use a negative value for no limit.
		 */
		private int maxActive = 8;

		/**
		 * Maximum amount of time a connection allocation should block before throwing an
		 * exception when the pool is exhausted. Use a negative value to block
		 * indefinitely.
		 */
		private Duration maxWait = Duration.ofMillis(-1);

		/**
		 * Time between runs of the idle object evictor thread. When positive, the idle
		 * object evictor thread starts, otherwise no idle object eviction is performed.
		 */
		private Duration timeBetweenEvictionRuns;

		/**
		 * Returns the value of the enabled flag.
		 * @return the value of the enabled flag
		 */
		public Boolean getEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the Pool.
		 * @param enabled the enabled status to be set
		 */
		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Returns the maximum number of idle objects that can be held in the pool.
		 * @return the maximum number of idle objects
		 */
		public int getMaxIdle() {
			return this.maxIdle;
		}

		/**
		 * Sets the maximum number of idle objects in the pool.
		 * @param maxIdle the maximum number of idle objects to set
		 */
		public void setMaxIdle(int maxIdle) {
			this.maxIdle = maxIdle;
		}

		/**
		 * Returns the minimum number of idle objects that should be maintained in the
		 * pool.
		 * @return the minimum number of idle objects
		 */
		public int getMinIdle() {
			return this.minIdle;
		}

		/**
		 * Sets the minimum number of idle objects in the pool.
		 * @param minIdle the minimum number of idle objects to set
		 */
		public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		/**
		 * Returns the maximum number of active objects that can be allocated by this
		 * pool.
		 * @return the maximum number of active objects
		 */
		public int getMaxActive() {
			return this.maxActive;
		}

		/**
		 * Sets the maximum number of active objects in the pool.
		 * @param maxActive the maximum number of active objects to set
		 */
		public void setMaxActive(int maxActive) {
			this.maxActive = maxActive;
		}

		/**
		 * Returns the maximum wait time for acquiring a resource from the pool.
		 * @return the maximum wait time as a Duration object
		 */
		public Duration getMaxWait() {
			return this.maxWait;
		}

		/**
		 * Sets the maximum wait time for acquiring a resource from the pool.
		 * @param maxWait the maximum wait time as a Duration object
		 */
		public void setMaxWait(Duration maxWait) {
			this.maxWait = maxWait;
		}

		/**
		 * Returns the time between eviction runs.
		 * @return the time between eviction runs
		 */
		public Duration getTimeBetweenEvictionRuns() {
			return this.timeBetweenEvictionRuns;
		}

		/**
		 * Sets the time between eviction runs for the pool.
		 * @param timeBetweenEvictionRuns the duration between eviction runs
		 */
		public void setTimeBetweenEvictionRuns(Duration timeBetweenEvictionRuns) {
			this.timeBetweenEvictionRuns = timeBetweenEvictionRuns;
		}

	}

	/**
	 * Cluster properties.
	 */
	public static class Cluster {

		/**
		 * Comma-separated list of "host:port" pairs to bootstrap from. This represents an
		 * "initial" list of cluster nodes and is required to have at least one entry.
		 */
		private List<String> nodes;

		/**
		 * Maximum number of redirects to follow when executing commands across the
		 * cluster.
		 */
		private Integer maxRedirects;

		/**
		 * Returns the list of nodes in the cluster.
		 * @return the list of nodes in the cluster
		 */
		public List<String> getNodes() {
			return this.nodes;
		}

		/**
		 * Sets the list of nodes in the cluster.
		 * @param nodes the list of nodes to be set
		 */
		public void setNodes(List<String> nodes) {
			this.nodes = nodes;
		}

		/**
		 * Returns the maximum number of redirects allowed for the Cluster.
		 * @return the maximum number of redirects
		 */
		public Integer getMaxRedirects() {
			return this.maxRedirects;
		}

		/**
		 * Sets the maximum number of redirects allowed for the cluster.
		 * @param maxRedirects the maximum number of redirects to be set
		 */
		public void setMaxRedirects(Integer maxRedirects) {
			this.maxRedirects = maxRedirects;
		}

	}

	/**
	 * Redis sentinel properties.
	 */
	public static class Sentinel {

		/**
		 * Name of the Redis server.
		 */
		private String master;

		/**
		 * Comma-separated list of "host:port" pairs.
		 */
		private List<String> nodes;

		/**
		 * Login username for authenticating with sentinel(s).
		 */
		private String username;

		/**
		 * Password for authenticating with sentinel(s).
		 */
		private String password;

		/**
		 * Returns the value of the master property.
		 * @return the value of the master property
		 */
		public String getMaster() {
			return this.master;
		}

		/**
		 * Sets the master for the Sentinel.
		 * @param master the master to be set
		 */
		public void setMaster(String master) {
			this.master = master;
		}

		/**
		 * Returns the list of nodes.
		 * @return the list of nodes
		 */
		public List<String> getNodes() {
			return this.nodes;
		}

		/**
		 * Sets the list of nodes for the Sentinel.
		 * @param nodes the list of nodes to be set
		 */
		public void setNodes(List<String> nodes) {
			this.nodes = nodes;
		}

		/**
		 * Returns the username of the Sentinel.
		 * @return the username of the Sentinel
		 */
		public String getUsername() {
			return this.username;
		}

		/**
		 * Sets the username for the Sentinel.
		 * @param username the username to be set
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * Returns the password of the Sentinel.
		 * @return the password of the Sentinel
		 */
		public String getPassword() {
			return this.password;
		}

		/**
		 * Sets the password for the Sentinel.
		 * @param password the password to be set
		 */
		public void setPassword(String password) {
			this.password = password;
		}

	}

	/**
	 * Ssl class.
	 */
	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		/**
		 * Returns a boolean value indicating whether the SSL is enabled.
		 * @return true if SSL is enabled, false otherwise
		 */
		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		/**
		 * Sets the enabled status of the Ssl object.
		 * @param enabled the boolean value indicating whether the Ssl object is enabled
		 * or not
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Returns the bundle associated with this Ssl object.
		 * @return the bundle associated with this Ssl object
		 */
		public String getBundle() {
			return this.bundle;
		}

		/**
		 * Sets the bundle for the Ssl class.
		 * @param bundle the bundle to be set
		 */
		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

	/**
	 * Jedis client properties.
	 */
	public static class Jedis {

		/**
		 * Jedis pool configuration.
		 */
		private final Pool pool = new Pool();

		/**
		 * Returns the pool associated with this Jedis instance.
		 * @return the pool associated with this Jedis instance
		 */
		public Pool getPool() {
			return this.pool;
		}

	}

	/**
	 * Lettuce client properties.
	 */
	public static class Lettuce {

		/**
		 * Shutdown timeout.
		 */
		private Duration shutdownTimeout = Duration.ofMillis(100);

		/**
		 * Lettuce pool configuration.
		 */
		private final Pool pool = new Pool();

		private final Cluster cluster = new Cluster();

		/**
		 * Returns the shutdown timeout duration.
		 * @return the shutdown timeout duration
		 */
		public Duration getShutdownTimeout() {
			return this.shutdownTimeout;
		}

		/**
		 * Sets the shutdown timeout for the Lettuce instance.
		 * @param shutdownTimeout the duration to wait for the Lettuce instance to
		 * shutdown gracefully
		 */
		public void setShutdownTimeout(Duration shutdownTimeout) {
			this.shutdownTimeout = shutdownTimeout;
		}

		/**
		 * Returns the pool associated with this Lettuce instance.
		 * @return the pool associated with this Lettuce instance
		 */
		public Pool getPool() {
			return this.pool;
		}

		/**
		 * Returns the cluster associated with this Lettuce instance.
		 * @return the cluster associated with this Lettuce instance
		 */
		public Cluster getCluster() {
			return this.cluster;
		}

		/**
		 * Cluster class.
		 */
		public static class Cluster {

			private final Refresh refresh = new Refresh();

			/**
			 * Returns the refresh object associated with this Cluster.
			 * @return the refresh object associated with this Cluster
			 */
			public Refresh getRefresh() {
				return this.refresh;
			}

			/**
			 * Refresh class.
			 */
			public static class Refresh {

				/**
				 * Whether to discover and query all cluster nodes for obtaining the
				 * cluster topology. When set to false, only the initial seed nodes are
				 * used as sources for topology discovery.
				 */
				private boolean dynamicRefreshSources = true;

				/**
				 * Cluster topology refresh period.
				 */
				private Duration period;

				/**
				 * Whether adaptive topology refreshing using all available refresh
				 * triggers should be used.
				 */
				private boolean adaptive;

				/**
				 * Returns a boolean value indicating whether dynamic refresh sources are
				 * enabled.
				 * @return true if dynamic refresh sources are enabled, false otherwise
				 */
				public boolean isDynamicRefreshSources() {
					return this.dynamicRefreshSources;
				}

				/**
				 * Sets the flag indicating whether dynamic refresh sources are enabled or
				 * not.
				 * @param dynamicRefreshSources the flag indicating whether dynamic
				 * refresh sources are enabled or not
				 */
				public void setDynamicRefreshSources(boolean dynamicRefreshSources) {
					this.dynamicRefreshSources = dynamicRefreshSources;
				}

				/**
				 * Returns the period of time between refreshes.
				 * @return the period of time between refreshes
				 */
				public Duration getPeriod() {
					return this.period;
				}

				/**
				 * Sets the period for refreshing.
				 * @param period the duration of the refresh period
				 */
				public void setPeriod(Duration period) {
					this.period = period;
				}

				/**
				 * Returns a boolean value indicating whether the Refresh object is
				 * adaptive.
				 * @return true if the Refresh object is adaptive, false otherwise
				 */
				public boolean isAdaptive() {
					return this.adaptive;
				}

				/**
				 * Sets the adaptive flag for the Refresh class.
				 * @param adaptive the value to set the adaptive flag to
				 */
				public void setAdaptive(boolean adaptive) {
					this.adaptive = adaptive;
				}

			}

		}

	}

}
