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
 * @author Yulin Qin
 * @author Brian Clozel
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.couchbase")
public class CouchbaseProperties {

	/**
	 * Couchbase nodes (host or IP address) to bootstrap from.
	 */
	private List<String> bootstrapHosts;

	/**
	 * Cluster username when using role based access.
	 */
	private String username;

	/**
	 * Cluster password when using role based access.
	 */
	private String password;

	private final Bucket bucket = new Bucket();

	private final Env env = new Env();

	public List<String> getBootstrapHosts() {
		return this.bootstrapHosts;
	}

	public void setBootstrapHosts(List<String> bootstrapHosts) {
		this.bootstrapHosts = bootstrapHosts;
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

		private final Bootstrap bootstrap = new Bootstrap();

		private final Endpoints endpoints = new Endpoints();

		private final Ssl ssl = new Ssl();

		private final Timeouts timeouts = new Timeouts();

		public Bootstrap getBootstrap() {
			return this.bootstrap;
		}

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
		 * Query (N1QL) service configuration.
		 */
		private final CouchbaseService queryservice = new CouchbaseService();

		/**
		 * View service configuration.
		 */
		private final CouchbaseService viewservice = new CouchbaseService();

		public int getKeyValue() {
			return this.keyValue;
		}

		public void setKeyValue(int keyValue) {
			this.keyValue = keyValue;
		}

		public CouchbaseService getQueryservice() {
			return this.queryservice;
		}

		public CouchbaseService getViewservice() {
			return this.viewservice;
		}

		public static class CouchbaseService {

			/**
			 * Minimum number of sockets per node.
			 */
			private int minEndpoints = 1;

			/**
			 * Maximum number of sockets per node.
			 */
			private int maxEndpoints = 1;

			public int getMinEndpoints() {
				return this.minEndpoints;
			}

			public void setMinEndpoints(int minEndpoints) {
				this.minEndpoints = minEndpoints;
			}

			public int getMaxEndpoints() {
				return this.maxEndpoints;
			}

			public void setMaxEndpoints(int maxEndpoints) {
				this.maxEndpoints = maxEndpoints;
			}

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
			return (this.enabled != null) ? this.enabled : StringUtils.hasText(this.keyStore);
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

	public static class Bootstrap {

		/**
		 * Port for the HTTP bootstrap.
		 */
		private Integer httpDirectPort;

		/**
		 * Port for the HTTPS bootstrap.
		 */
		private Integer httpSslPort;

		public Integer getHttpDirectPort() {
			return this.httpDirectPort;
		}

		public void setHttpDirectPort(Integer httpDirectPort) {
			this.httpDirectPort = httpDirectPort;
		}

		public Integer getHttpSslPort() {
			return this.httpSslPort;
		}

		public void setHttpSslPort(Integer httpSslPort) {
			this.httpSslPort = httpSslPort;
		}

	}

}
