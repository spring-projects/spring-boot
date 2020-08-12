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

package org.springframework.boot.autoconfigure.neo4j;

import java.io.File;
import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Neo4j.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @since 2.4.0
 */
@ConfigurationProperties(prefix = "spring.neo4j")
public class Neo4jProperties {

	/**
	 * URI used by the driver.
	 */
	private URI uri;

	/**
	 * Timeout for borrowing connections from the pool.
	 */
	private Duration connectionTimeout = Duration.ofSeconds(30);

	/**
	 * Maximum time transactions are allowed to retry.
	 */
	private Duration maxTransactionRetryTime = Duration.ofSeconds(30);

	private final Authentication authentication = new Authentication();

	private final Pool pool = new Pool();

	private final Security security = new Security();

	public URI getUri() {
		return this.uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Duration getMaxTransactionRetryTime() {
		return this.maxTransactionRetryTime;
	}

	public void setMaxTransactionRetryTime(Duration maxTransactionRetryTime) {
		this.maxTransactionRetryTime = maxTransactionRetryTime;
	}

	public Authentication getAuthentication() {
		return this.authentication;
	}

	public Pool getPool() {
		return this.pool;
	}

	public Security getSecurity() {
		return this.security;
	}

	public static class Authentication {

		/**
		 * Login user of the server.
		 */
		private String username;

		/**
		 * Login password of the server.
		 */
		private String password;

		/**
		 * Realm to connect to.
		 */
		private String realm;

		/**
		 * Kerberos ticket for connecting to the database. Mutual exclusive with a given
		 * username.
		 */
		private String kerberosTicket;

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

		public String getRealm() {
			return this.realm;
		}

		public void setRealm(String realm) {
			this.realm = realm;
		}

		public String getKerberosTicket() {
			return this.kerberosTicket;
		}

		public void setKerberosTicket(String kerberosTicket) {
			this.kerberosTicket = kerberosTicket;
		}

	}

	public static class Pool {

		/**
		 * Whether to enable metrics.
		 */
		private boolean metricsEnabled = false;

		/**
		 * Whether to log leaked sessions.
		 */
		private boolean logLeakedSessions = false;

		/**
		 * Maximum amount of connections in the connection pool towards a single database.
		 */
		private int maxConnectionPoolSize = 100;

		/**
		 * Pooled connections that have been idle in the pool for longer than this
		 * threshold will be tested before they are used again.
		 */
		private Duration idleTimeBeforeConnectionTest;

		/**
		 * Pooled connections older than this threshold will be closed and removed from
		 * the pool.
		 */
		private Duration maxConnectionLifetime = Duration.ofHours(1);

		/**
		 * Acquisition of new connections will be attempted for at most configured
		 * timeout.
		 */
		private Duration connectionAcquisitionTimeout = Duration.ofSeconds(60);

		public boolean isLogLeakedSessions() {
			return this.logLeakedSessions;
		}

		public void setLogLeakedSessions(boolean logLeakedSessions) {
			this.logLeakedSessions = logLeakedSessions;
		}

		public int getMaxConnectionPoolSize() {
			return this.maxConnectionPoolSize;
		}

		public void setMaxConnectionPoolSize(int maxConnectionPoolSize) {
			this.maxConnectionPoolSize = maxConnectionPoolSize;
		}

		public Duration getIdleTimeBeforeConnectionTest() {
			return this.idleTimeBeforeConnectionTest;
		}

		public void setIdleTimeBeforeConnectionTest(Duration idleTimeBeforeConnectionTest) {
			this.idleTimeBeforeConnectionTest = idleTimeBeforeConnectionTest;
		}

		public Duration getMaxConnectionLifetime() {
			return this.maxConnectionLifetime;
		}

		public void setMaxConnectionLifetime(Duration maxConnectionLifetime) {
			this.maxConnectionLifetime = maxConnectionLifetime;
		}

		public Duration getConnectionAcquisitionTimeout() {
			return this.connectionAcquisitionTimeout;
		}

		public void setConnectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
			this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
		}

		public boolean isMetricsEnabled() {
			return this.metricsEnabled;
		}

		public void setMetricsEnabled(boolean metricsEnabled) {
			this.metricsEnabled = metricsEnabled;
		}

	}

	public static class Security {

		/**
		 * Whether the driver should use encrypted traffic.
		 */
		private boolean encrypted = false;

		/**
		 * Trust strategy to use.
		 */
		private TrustStrategy trustStrategy = TrustStrategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES;

		/**
		 * Path to the file that holds the trusted certificates.
		 */
		private File certFile;

		/**
		 * Whether hostname verification is required.
		 */
		private boolean hostnameVerificationEnabled = true;

		public boolean isEncrypted() {
			return this.encrypted;
		}

		public void setEncrypted(boolean encrypted) {
			this.encrypted = encrypted;
		}

		public TrustStrategy getTrustStrategy() {
			return this.trustStrategy;
		}

		public void setTrustStrategy(TrustStrategy trustStrategy) {
			this.trustStrategy = trustStrategy;
		}

		public File getCertFile() {
			return this.certFile;
		}

		public void setCertFile(File certFile) {
			this.certFile = certFile;
		}

		public boolean isHostnameVerificationEnabled() {
			return this.hostnameVerificationEnabled;
		}

		public void setHostnameVerificationEnabled(boolean hostnameVerificationEnabled) {
			this.hostnameVerificationEnabled = hostnameVerificationEnabled;
		}

		public enum TrustStrategy {

			/**
			 * Trust all certificates.
			 */
			TRUST_ALL_CERTIFICATES,

			/**
			 * Trust certificates that are signed by a trusted certificate.
			 */
			TRUST_CUSTOM_CA_SIGNED_CERTIFICATES,

			/**
			 * Trust certificates that can be verified through the local system store.
			 */
			TRUST_SYSTEM_CA_SIGNED_CERTIFICATES

		}

	}

}
