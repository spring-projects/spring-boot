/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.couchbase")
public class CouchbaseProperties {

	/**
	 * Couchbase nodes (host or IP address) to bootstrap from.
	 */
	private List<String> bootstrapHosts;

	private final Bucket bucket = new Bucket();

	private final Env env = new Env();

	public List<String> getBootstrapHosts() {
		return this.bootstrapHosts;
	}

	public void setBootstrapHosts(List<String> bootstrapHosts) {
		this.bootstrapHosts = bootstrapHosts;
	}

	public Bucket getBucket() {
		return this.bucket;
	}

	public Env getEnv() {
		return this.env;
	}

	public static class Bucket {

		/**
		 * Name of the bucket to connect to.
		 */
		private String name = "default";

		/**
		 * Password of the bucket.
		 */
		private String password = "";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

	public static class Env {

		private final Endpoints endpoints = new Endpoints();

		private final Ssl ssl = new Ssl();

		private final Timeouts timeouts = new Timeouts();

		public Endpoints getEndpoints() {
			return this.endpoints;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public Timeouts getTimeouts() {
			return this.timeouts;
		}

	}

	public static class Endpoints {

		/**
		 * Number of sockets per node against the key/value service.
		 */
		private int keyValue = 1;

		/**
		 * Number of sockets per node against the query (N1QL) service.
		 */
		private int query = 1;

		/**
		 * Number of sockets per node against the view service.
		 */
		private int view = 1;

		public int getKeyValue() {
			return this.keyValue;
		}

		public void setKeyValue(int keyValue) {
			this.keyValue = keyValue;
		}

		public int getQuery() {
			return this.query;
		}

		public void setQuery(int query) {
			this.query = query;
		}

		public int getView() {
			return this.view;
		}

		public void setView(int view) {
			this.view = view;
		}

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if a "keyStore" is
		 * provided unless specified otherwise.
		 */
		private Boolean enabled;

		/**
		 * Path to the JVM key store that holds the certificates.
		 */
		private String keyStore;

		/**
		 * Password used to access the key store.
		 */
		private String keyStorePassword;

		public Boolean getEnabled() {
			return (this.enabled != null ? this.enabled
					: StringUtils.hasText(this.keyStore));
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public String getKeyStore() {
			return this.keyStore;
		}

		public void setKeyStore(String keyStore) {
			this.keyStore = keyStore;
		}

		public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		public void setKeyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

	}

	public static class Timeouts {

		/**
		 * Bucket connections timeouts.
		 */
		private Duration connect = Duration.ofMillis(5000);

		/**
		 * Blocking operations performed on a specific key timeout.
		 */
		private Duration keyValue = Duration.ofMillis(2500);

		/**
		 * N1QL query operations timeout.
		 */
		private Duration query = Duration.ofMillis(7500);

		/**
		 * Socket connect connections timeout.
		 */
		private Duration socketConnect = Duration.ofMillis(1000);

		/**
		 * Regular and geospatial view operations timeout.
		 */
		private Duration view = Duration.ofMillis(7500);

		public Duration getConnect() {
			return this.connect;
		}

		public void setConnect(Duration connect) {
			this.connect = connect;
		}

		public Duration getKeyValue() {
			return this.keyValue;
		}

		public void setKeyValue(Duration keyValue) {
			this.keyValue = keyValue;
		}

		public Duration getQuery() {
			return this.query;
		}

		public void setQuery(Duration query) {
			this.query = query;
		}

		public Duration getSocketConnect() {
			return this.socketConnect;
		}

		public void setSocketConnect(Duration socketConnect) {
			this.socketConnect = socketConnect;
		}

		public Duration getView() {
			return this.view;
		}

		public void setView(Duration view) {
			this.view = view;
		}

	}

}
