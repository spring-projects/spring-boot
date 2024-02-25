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

package org.springframework.boot.autoconfigure.couchbase;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Yulin Qin
 * @author Brian Clozel
 * @author Michael Nitschinger
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.couchbase")
public class CouchbaseProperties {

	/**
	 * Connection string used to locate the Couchbase cluster.
	 */
	private String connectionString;

	/**
	 * Cluster username.
	 */
	private String username;

	/**
	 * Cluster password.
	 */
	private String password;

	private final Env env = new Env();

	/**
	 * Returns the connection string for the Couchbase database.
	 * @return the connection string
	 */
	public String getConnectionString() {
		return this.connectionString;
	}

	/**
	 * Sets the connection string for the Couchbase database.
	 * @param connectionString the connection string to be set
	 */
	public void setConnectionString(String connectionString) {
		this.connectionString = connectionString;
	}

	/**
	 * Returns the username associated with the Couchbase properties.
	 * @return the username associated with the Couchbase properties
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username for the Couchbase connection.
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the password associated with the CouchbaseProperties object.
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the Couchbase connection.
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the environment associated with the CouchbaseProperties object.
	 * @return the environment associated with the CouchbaseProperties object
	 */
	public Env getEnv() {
		return this.env;
	}

	/**
	 * Env class.
	 */
	public static class Env {

		private final Io io = new Io();

		private final Ssl ssl = new Ssl();

		private final Timeouts timeouts = new Timeouts();

		/**
		 * Returns the Io object associated with this Env instance.
		 * @return the Io object associated with this Env instance
		 */
		public Io getIo() {
			return this.io;
		}

		/**
		 * Returns the SSL object associated with this environment.
		 * @return the SSL object
		 */
		public Ssl getSsl() {
			return this.ssl;
		}

		/**
		 * Returns the Timeouts object associated with this Env instance.
		 * @return the Timeouts object associated with this Env instance
		 */
		public Timeouts getTimeouts() {
			return this.timeouts;
		}

	}

	/**
	 * Io class.
	 */
	public static class Io {

		/**
		 * Minimum number of sockets per node.
		 */
		private int minEndpoints = 1;

		/**
		 * Maximum number of sockets per node.
		 */
		private int maxEndpoints = 12;

		/**
		 * Length of time an HTTP connection may remain idle before it is closed and
		 * removed from the pool.
		 */
		private Duration idleHttpConnectionTimeout = Duration.ofSeconds(1);

		/**
		 * Returns the minimum number of endpoints.
		 * @return the minimum number of endpoints
		 */
		public int getMinEndpoints() {
			return this.minEndpoints;
		}

		/**
		 * Sets the minimum number of endpoints for the Io class.
		 * @param minEndpoints the minimum number of endpoints to be set
		 */
		public void setMinEndpoints(int minEndpoints) {
			this.minEndpoints = minEndpoints;
		}

		/**
		 * Returns the maximum number of endpoints.
		 * @return the maximum number of endpoints
		 */
		public int getMaxEndpoints() {
			return this.maxEndpoints;
		}

		/**
		 * Sets the maximum number of endpoints.
		 * @param maxEndpoints the maximum number of endpoints to be set
		 */
		public void setMaxEndpoints(int maxEndpoints) {
			this.maxEndpoints = maxEndpoints;
		}

		/**
		 * Returns the idle HTTP connection timeout.
		 * @return the idle HTTP connection timeout
		 */
		public Duration getIdleHttpConnectionTimeout() {
			return this.idleHttpConnectionTimeout;
		}

		/**
		 * Sets the idle HTTP connection timeout.
		 * @param idleHttpConnectionTimeout the duration of idle HTTP connection timeout
		 */
		public void setIdleHttpConnectionTimeout(Duration idleHttpConnectionTimeout) {
			this.idleHttpConnectionTimeout = idleHttpConnectionTimeout;
		}

	}

	/**
	 * Ssl class.
	 */
	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if a "bundle" is provided
		 * unless specified otherwise.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		/**
		 * Returns the value of the enabled property.
		 * @return {@code true} if the enabled property is not null, or if the bundle
		 * property has text; {@code false} otherwise.
		 */
		public Boolean getEnabled() {
			return (this.enabled != null) ? this.enabled : StringUtils.hasText(this.bundle);
		}

		/**
		 * Sets the enabled status of the SSL.
		 * @param enabled the enabled status to be set
		 */
		public void setEnabled(Boolean enabled) {
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
	 * Timeouts class.
	 */
	public static class Timeouts {

		/**
		 * Bucket connect timeout.
		 */
		private Duration connect = Duration.ofSeconds(10);

		/**
		 * Bucket disconnect timeout.
		 */
		private Duration disconnect = Duration.ofSeconds(10);

		/**
		 * Timeout for operations on a specific key-value.
		 */
		private Duration keyValue = Duration.ofMillis(2500);

		/**
		 * Timeout for operations on a specific key-value with a durability level.
		 */
		private Duration keyValueDurable = Duration.ofSeconds(10);

		/**
		 * N1QL query operations timeout.
		 */
		private Duration query = Duration.ofSeconds(75);

		/**
		 * Regular and geospatial view operations timeout.
		 */
		private Duration view = Duration.ofSeconds(75);

		/**
		 * Timeout for the search service.
		 */
		private Duration search = Duration.ofSeconds(75);

		/**
		 * Timeout for the analytics service.
		 */
		private Duration analytics = Duration.ofSeconds(75);

		/**
		 * Timeout for the management operations.
		 */
		private Duration management = Duration.ofSeconds(75);

		/**
		 * Returns the duration for establishing a connection.
		 * @return the duration for establishing a connection
		 */
		public Duration getConnect() {
			return this.connect;
		}

		/**
		 * Sets the duration for establishing a connection.
		 * @param connect the duration for establishing a connection
		 */
		public void setConnect(Duration connect) {
			this.connect = connect;
		}

		/**
		 * Returns the duration of the disconnect timeout.
		 * @return the duration of the disconnect timeout
		 */
		public Duration getDisconnect() {
			return this.disconnect;
		}

		/**
		 * Sets the duration for disconnect timeout.
		 * @param disconnect the duration for disconnect timeout
		 */
		public void setDisconnect(Duration disconnect) {
			this.disconnect = disconnect;
		}

		/**
		 * Returns the value of the key.
		 * @return the value of the key
		 */
		public Duration getKeyValue() {
			return this.keyValue;
		}

		/**
		 * Sets the key value for the duration.
		 * @param keyValue the key value to be set
		 */
		public void setKeyValue(Duration keyValue) {
			this.keyValue = keyValue;
		}

		/**
		 * Returns the duration of the key-value durable timeout.
		 * @return the duration of the key-value durable timeout
		 */
		public Duration getKeyValueDurable() {
			return this.keyValueDurable;
		}

		/**
		 * Sets the duration for key-value durability.
		 * @param keyValueDurable the duration for key-value durability
		 */
		public void setKeyValueDurable(Duration keyValueDurable) {
			this.keyValueDurable = keyValueDurable;
		}

		/**
		 * Returns the query duration.
		 * @return the query duration
		 */
		public Duration getQuery() {
			return this.query;
		}

		/**
		 * Sets the duration of the query timeout.
		 * @param query the duration of the query timeout
		 */
		public void setQuery(Duration query) {
			this.query = query;
		}

		/**
		 * Returns the view duration.
		 * @return the view duration
		 */
		public Duration getView() {
			return this.view;
		}

		/**
		 * Sets the duration of the view.
		 * @param view the duration of the view
		 */
		public void setView(Duration view) {
			this.view = view;
		}

		/**
		 * Returns the search duration.
		 * @return the search duration
		 */
		public Duration getSearch() {
			return this.search;
		}

		/**
		 * Sets the duration for the search timeout.
		 * @param search the duration for the search timeout
		 */
		public void setSearch(Duration search) {
			this.search = search;
		}

		/**
		 * Returns the analytics duration.
		 * @return the analytics duration
		 */
		public Duration getAnalytics() {
			return this.analytics;
		}

		/**
		 * Sets the duration for analytics.
		 * @param analytics the duration for analytics
		 */
		public void setAnalytics(Duration analytics) {
			this.analytics = analytics;
		}

		/**
		 * Returns the management duration.
		 * @return the management duration
		 */
		public Duration getManagement() {
			return this.management;
		}

		/**
		 * Sets the duration for management.
		 * @param management the duration for management
		 */
		public void setManagement(Duration management) {
			this.management = management;
		}

	}

}
