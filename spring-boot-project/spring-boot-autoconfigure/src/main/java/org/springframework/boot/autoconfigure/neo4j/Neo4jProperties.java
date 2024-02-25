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

	/**
	 * Returns the URI of the Neo4j database.
	 * @return the URI of the Neo4j database
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Sets the URI for the Neo4j database.
	 * @param uri the URI to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * Returns the connection timeout duration.
	 * @return the connection timeout duration
	 */
	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	/**
	 * Sets the connection timeout for the Neo4j database.
	 * @param connectionTimeout the duration of the connection timeout
	 */
	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Returns the maximum transaction retry time.
	 * @return the maximum transaction retry time
	 */
	public Duration getMaxTransactionRetryTime() {
		return this.maxTransactionRetryTime;
	}

	/**
	 * Sets the maximum transaction retry time.
	 * @param maxTransactionRetryTime the maximum transaction retry time to set
	 */
	public void setMaxTransactionRetryTime(Duration maxTransactionRetryTime) {
		this.maxTransactionRetryTime = maxTransactionRetryTime;
	}

	/**
	 * Returns the authentication object associated with this Neo4jProperties instance.
	 * @return the authentication object
	 */
	public Authentication getAuthentication() {
		return this.authentication;
	}

	/**
	 * Returns the pool associated with this Neo4jProperties object.
	 * @return the pool associated with this Neo4jProperties object
	 */
	public Pool getPool() {
		return this.pool;
	}

	/**
	 * Returns the security configuration of the Neo4jProperties.
	 * @return the security configuration of the Neo4jProperties
	 */
	public Security getSecurity() {
		return this.security;
	}

	/**
	 * Authentication class.
	 */
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

		/**
		 * Returns the username associated with the Authentication object.
		 * @return the username
		 */
		public String getUsername() {
			return this.username;
		}

		/**
		 * Sets the username for authentication.
		 * @param username the username to be set
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * Returns the password of the Authentication object.
		 * @return the password of the Authentication object
		 */
		public String getPassword() {
			return this.password;
		}

		/**
		 * Sets the password for the user.
		 * @param password the password to be set
		 */
		public void setPassword(String password) {
			this.password = password;
		}

		/**
		 * Returns the realm of the authentication.
		 * @return the realm of the authentication
		 */
		public String getRealm() {
			return this.realm;
		}

		/**
		 * Sets the realm for authentication.
		 * @param realm the realm to be set
		 */
		public void setRealm(String realm) {
			this.realm = realm;
		}

		/**
		 * Returns the Kerberos ticket associated with the authentication.
		 * @return the Kerberos ticket
		 */
		public String getKerberosTicket() {
			return this.kerberosTicket;
		}

		/**
		 * Sets the Kerberos ticket for authentication.
		 * @param kerberosTicket the Kerberos ticket to be set
		 */
		public void setKerberosTicket(String kerberosTicket) {
			this.kerberosTicket = kerberosTicket;
		}

	}

	/**
	 * Pool class.
	 */
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

		/**
		 * Returns a boolean value indicating whether or not leaked sessions are logged.
		 * @return true if leaked sessions are logged, false otherwise
		 */
		public boolean isLogLeakedSessions() {
			return this.logLeakedSessions;
		}

		/**
		 * Sets the flag to enable or disable logging of leaked sessions.
		 * @param logLeakedSessions true to enable logging of leaked sessions, false
		 * otherwise
		 */
		public void setLogLeakedSessions(boolean logLeakedSessions) {
			this.logLeakedSessions = logLeakedSessions;
		}

		/**
		 * Returns the maximum size of the connection pool.
		 * @return the maximum size of the connection pool
		 */
		public int getMaxConnectionPoolSize() {
			return this.maxConnectionPoolSize;
		}

		/**
		 * Sets the maximum size of the connection pool.
		 * @param maxConnectionPoolSize the maximum size of the connection pool
		 */
		public void setMaxConnectionPoolSize(int maxConnectionPoolSize) {
			this.maxConnectionPoolSize = maxConnectionPoolSize;
		}

		/**
		 * Returns the idle time before connection test.
		 * @return the idle time before connection test
		 */
		public Duration getIdleTimeBeforeConnectionTest() {
			return this.idleTimeBeforeConnectionTest;
		}

		/**
		 * Sets the idle time before connection test.
		 * @param idleTimeBeforeConnectionTest the duration of idle time before connection
		 * test
		 */
		public void setIdleTimeBeforeConnectionTest(Duration idleTimeBeforeConnectionTest) {
			this.idleTimeBeforeConnectionTest = idleTimeBeforeConnectionTest;
		}

		/**
		 * Returns the maximum lifetime of a connection in the pool.
		 * @return the maximum lifetime of a connection
		 */
		public Duration getMaxConnectionLifetime() {
			return this.maxConnectionLifetime;
		}

		/**
		 * Sets the maximum lifetime of a connection in the pool.
		 * @param maxConnectionLifetime the maximum lifetime of a connection
		 */
		public void setMaxConnectionLifetime(Duration maxConnectionLifetime) {
			this.maxConnectionLifetime = maxConnectionLifetime;
		}

		/**
		 * Returns the connection acquisition timeout.
		 * @return the connection acquisition timeout
		 */
		public Duration getConnectionAcquisitionTimeout() {
			return this.connectionAcquisitionTimeout;
		}

		/**
		 * Sets the timeout for acquiring a connection from the pool.
		 * @param connectionAcquisitionTimeout the timeout duration for acquiring a
		 * connection
		 */
		public void setConnectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
			this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
		}

		/**
		 * Returns a boolean value indicating whether metrics are enabled for the Pool.
		 * @return true if metrics are enabled, false otherwise
		 */
		public boolean isMetricsEnabled() {
			return this.metricsEnabled;
		}

		/**
		 * Sets whether metrics are enabled for the Pool.
		 * @param metricsEnabled true if metrics are enabled, false otherwise
		 */
		public void setMetricsEnabled(boolean metricsEnabled) {
			this.metricsEnabled = metricsEnabled;
		}

	}

	/**
	 * Security class.
	 */
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

		/**
		 * Returns a boolean value indicating whether the data is encrypted.
		 * @return true if the data is encrypted, false otherwise.
		 */
		public boolean isEncrypted() {
			return this.encrypted;
		}

		/**
		 * Sets the encrypted flag for the Security class.
		 * @param encrypted true if the data is encrypted, false otherwise
		 */
		public void setEncrypted(boolean encrypted) {
			this.encrypted = encrypted;
		}

		/**
		 * Returns the trust strategy used by the Security class.
		 * @return the trust strategy used by the Security class
		 */
		public TrustStrategy getTrustStrategy() {
			return this.trustStrategy;
		}

		/**
		 * Sets the trust strategy for the security.
		 * @param trustStrategy the trust strategy to be set
		 */
		public void setTrustStrategy(TrustStrategy trustStrategy) {
			this.trustStrategy = trustStrategy;
		}

		/**
		 * Returns the certificate file.
		 * @return the certificate file
		 */
		public File getCertFile() {
			return this.certFile;
		}

		/**
		 * Sets the certificate file for the security class.
		 * @param certFile the certificate file to be set
		 */
		public void setCertFile(File certFile) {
			this.certFile = certFile;
		}

		/**
		 * Returns the status of the hostname verification.
		 * @return true if hostname verification is enabled, false otherwise.
		 */
		public boolean isHostnameVerificationEnabled() {
			return this.hostnameVerificationEnabled;
		}

		/**
		 * Sets the flag indicating whether hostname verification is enabled.
		 * @param hostnameVerificationEnabled the flag indicating whether hostname
		 * verification is enabled
		 */
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
