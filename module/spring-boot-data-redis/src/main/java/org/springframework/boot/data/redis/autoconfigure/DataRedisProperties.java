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

package org.springframework.boot.data.redis.autoconfigure;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.Nullable;

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
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.redis")
public class DataRedisProperties {

	/**
	 * Database index used by the connection factory.
	 */
	private int database = 0;

	/**
	 * Connection URL. Overrides host, port, username, password, and database. Example:
	 * redis://user:password@example.com:6379/8
	 */
	private @Nullable String url;

	/**
	 * Redis server host.
	 */
	private String host = "localhost";

	/**
	 * Login username of the redis server.
	 */
	private @Nullable String username;

	/**
	 * Login password of the redis server.
	 */
	private @Nullable String password;

	/**
	 * Redis server port.
	 */
	private int port = 6379;

	/**
	 * Read timeout.
	 */
	private @Nullable Duration timeout;

	/**
	 * Connection timeout.
	 */
	private @Nullable Duration connectTimeout;

	/**
	 * Client name to be set on connections with CLIENT SETNAME.
	 */
	private @Nullable String clientName;

	/**
	 * Type of client to use. By default, auto-detected according to the classpath.
	 */
	private @Nullable ClientType clientType;

	private @Nullable Sentinel sentinel;

	private @Nullable Cluster cluster;

	private @Nullable Masterreplica masterreplica;

	private final Ssl ssl = new Ssl();

	private final Jedis jedis = new Jedis();

	private final Lettuce lettuce = new Lettuce();

	public int getDatabase() {
		return this.database;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public @Nullable String getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
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

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public @Nullable Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
	}

	public @Nullable Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(@Nullable Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public @Nullable String getClientName() {
		return this.clientName;
	}

	public void setClientName(@Nullable String clientName) {
		this.clientName = clientName;
	}

	public @Nullable ClientType getClientType() {
		return this.clientType;
	}

	public void setClientType(@Nullable ClientType clientType) {
		this.clientType = clientType;
	}

	public @Nullable Sentinel getSentinel() {
		return this.sentinel;
	}

	public void setSentinel(@Nullable Sentinel sentinel) {
		this.sentinel = sentinel;
	}

	public @Nullable Cluster getCluster() {
		return this.cluster;
	}

	public void setCluster(@Nullable Cluster cluster) {
		this.cluster = cluster;
	}

	public @Nullable Masterreplica getMasterreplica() {
		return this.masterreplica;
	}

	public void setMasterreplica(@Nullable Masterreplica masterreplica) {
		this.masterreplica = masterreplica;
	}

	public Jedis getJedis() {
		return this.jedis;
	}

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
		private @Nullable Boolean enabled;

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
		private @Nullable Duration timeBetweenEvictionRuns;

		public @Nullable Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(@Nullable Boolean enabled) {
			this.enabled = enabled;
		}

		public int getMaxIdle() {
			return this.maxIdle;
		}

		public void setMaxIdle(int maxIdle) {
			this.maxIdle = maxIdle;
		}

		public int getMinIdle() {
			return this.minIdle;
		}

		public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		public int getMaxActive() {
			return this.maxActive;
		}

		public void setMaxActive(int maxActive) {
			this.maxActive = maxActive;
		}

		public Duration getMaxWait() {
			return this.maxWait;
		}

		public void setMaxWait(Duration maxWait) {
			this.maxWait = maxWait;
		}

		public @Nullable Duration getTimeBetweenEvictionRuns() {
			return this.timeBetweenEvictionRuns;
		}

		public void setTimeBetweenEvictionRuns(@Nullable Duration timeBetweenEvictionRuns) {
			this.timeBetweenEvictionRuns = timeBetweenEvictionRuns;
		}

	}

	/**
	 * Cluster properties.
	 */
	public static class Cluster {

		/**
		 * List of "host:port" pairs to bootstrap from. This represents an "initial" list
		 * of cluster nodes and is required to have at least one entry.
		 */
		private @Nullable List<String> nodes;

		/**
		 * Maximum number of redirects to follow when executing commands across the
		 * cluster.
		 */
		private @Nullable Integer maxRedirects;

		public @Nullable List<String> getNodes() {
			return this.nodes;
		}

		public void setNodes(@Nullable List<String> nodes) {
			this.nodes = nodes;
		}

		public @Nullable Integer getMaxRedirects() {
			return this.maxRedirects;
		}

		public void setMaxRedirects(@Nullable Integer maxRedirects) {
			this.maxRedirects = maxRedirects;
		}

	}

	/**
	 * Master Replica properties.
	 */
	public static class Masterreplica {

		/**
		 * Static list of "host:port" pairs to use, at least one entry is required.
		 */
		private @Nullable List<String> nodes;

		public @Nullable List<String> getNodes() {
			return this.nodes;
		}

		public void setNodes(@Nullable List<String> nodes) {
			this.nodes = nodes;
		}

	}

	/**
	 * Redis sentinel properties.
	 */
	public static class Sentinel {

		/**
		 * Name of the Redis server.
		 */
		private @Nullable String master;

		/**
		 * List of "host:port" pairs.
		 */
		private @Nullable List<String> nodes;

		/**
		 * Login username for authenticating with sentinel(s).
		 */
		private @Nullable String username;

		/**
		 * Password for authenticating with sentinel(s).
		 */
		private @Nullable String password;

		public @Nullable String getMaster() {
			return this.master;
		}

		public void setMaster(@Nullable String master) {
			this.master = master;
		}

		public @Nullable List<String> getNodes() {
			return this.nodes;
		}

		public void setNodes(@Nullable List<String> nodes) {
			this.nodes = nodes;
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

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
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

	/**
	 * Jedis client properties.
	 */
	public static class Jedis {

		/**
		 * Jedis pool configuration.
		 */
		private final Pool pool = new Pool();

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
		 * Defines from which Redis nodes data is read.
		 */
		private @Nullable String readFrom;

		/**
		 * Lettuce pool configuration.
		 */
		private final Pool pool = new Pool();

		private final Cluster cluster = new Cluster();

		public Duration getShutdownTimeout() {
			return this.shutdownTimeout;
		}

		public void setShutdownTimeout(Duration shutdownTimeout) {
			this.shutdownTimeout = shutdownTimeout;
		}

		public @Nullable String getReadFrom() {
			return this.readFrom;
		}

		public void setReadFrom(@Nullable String readFrom) {
			this.readFrom = readFrom;
		}

		public Pool getPool() {
			return this.pool;
		}

		public Cluster getCluster() {
			return this.cluster;
		}

		public static class Cluster {

			private final Refresh refresh = new Refresh();

			public Refresh getRefresh() {
				return this.refresh;
			}

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
				private @Nullable Duration period;

				/**
				 * Whether adaptive topology refreshing using all available refresh
				 * triggers should be used.
				 */
				private boolean adaptive;

				public boolean isDynamicRefreshSources() {
					return this.dynamicRefreshSources;
				}

				public void setDynamicRefreshSources(boolean dynamicRefreshSources) {
					this.dynamicRefreshSources = dynamicRefreshSources;
				}

				public @Nullable Duration getPeriod() {
					return this.period;
				}

				public void setPeriod(@Nullable Duration period) {
					this.period = period;
				}

				public boolean isAdaptive() {
					return this.adaptive;
				}

				public void setAdaptive(boolean adaptive) {
					this.adaptive = adaptive;
				}

			}

		}

	}

}
