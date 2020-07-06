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

import org.neo4j.driver.net.ServerAddressResolver;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Used to configure an instance of the {@link org.neo4j.driver.Driver Neo4j-Java-Driver}.
 *
 * @author Michael J. Simons
 * @since 2.4.0
 */
@ConfigurationProperties(prefix = "spring.neo4j")
public class Neo4jProperties {

	/**
	 * Uri this driver should connect to. The driver supports bolt or neo4j as schemes.
	 */
	private URI uri = URI.create("bolt://localhost:7687");

	/**
	 * Authentication the driver is supposed to use. Maybe null.
	 */
	private Authentication authentication = new Authentication();

	/**
	 * Configuration of the connection pool.
	 */
	private PoolSettings pool = new PoolSettings();

	/**
	 * Detailed configuration of the driver.
	 */
	private DriverSettings config = new DriverSettings();

	public URI getUri() {
		return this.uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Authentication getAuthentication() {
		return this.authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	public PoolSettings getPool() {
		return this.pool;
	}

	public void setPool(PoolSettings pool) {
		this.pool = pool;
	}

	public DriverSettings getConfig() {
		return this.config;
	}

	public void setConfig(DriverSettings config) {
		this.config = config;
	}

	public static class Authentication {

		/**
		 * Login of the user connecting to the database.
		 */
		private String username;

		/**
		 * Password of the user connecting to the database.
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

	public static class PoolSettings {

		/**
		 * Flag, if metrics are enabled.
		 */
		private boolean metricsEnabled = false;

		/**
		 * Flag, if leaked sessions logging is enabled.
		 */
		private boolean logLeakedSessions = false;

		/**
		 * Maximum amount of connections in the connection pool towards a single database.
		 */
		private int maxConnectionPoolSize = org.neo4j.driver.internal.async.pool.PoolSettings.DEFAULT_MAX_CONNECTION_POOL_SIZE;

		/**
		 * Pooled connections that have been idle in the pool for longer than this timeout
		 * will be tested before they are used again.
		 */
		private Duration idleTimeBeforeConnectionTest;

		/**
		 * Pooled connections older than this threshold will be closed and removed from
		 * the pool.
		 */
		private Duration maxConnectionLifetime = Duration
				.ofMillis(org.neo4j.driver.internal.async.pool.PoolSettings.DEFAULT_MAX_CONNECTION_LIFETIME);

		/**
		 * Acquisition of new connections will be attempted for at most configured
		 * timeout.
		 */
		private Duration connectionAcquisitionTimeout = Duration
				.ofMillis(org.neo4j.driver.internal.async.pool.PoolSettings.DEFAULT_CONNECTION_ACQUISITION_TIMEOUT);

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

	public static class DriverSettings {

		/**
		 * Flag, if the driver should use encrypted traffic.
		 */
		private boolean encrypted = false;

		/**
		 * Specify how to determine the authenticity of an encryption certificate provided
		 * by the Neo4j instance we are connecting to.
		 */
		private TrustSettings trustSettings = new TrustSettings();

		/**
		 * Specify socket connection timeout.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(30);

		/**
		 * Specify the maximum time transactions are allowed to retry.
		 */
		private Duration maxTransactionRetryTime = Duration
				.ofMillis(org.neo4j.driver.internal.retry.RetrySettings.DEFAULT.maxRetryTimeMs());

		/**
		 * Specify a custom server address resolver used by the routing driver to resolve
		 * the initial address used to create the driver.
		 */
		private Class<? extends ServerAddressResolver> serverAddressResolverClass;

		public boolean isEncrypted() {
			return this.encrypted;
		}

		public void setEncrypted(boolean encrypted) {
			this.encrypted = encrypted;
		}

		public TrustSettings getTrustSettings() {
			return this.trustSettings;
		}

		public void setTrustSettings(TrustSettings trustSettings) {
			this.trustSettings = trustSettings;
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

		public Class<? extends ServerAddressResolver> getServerAddressResolverClass() {
			return this.serverAddressResolverClass;
		}

		public void setServerAddressResolverClass(Class<? extends ServerAddressResolver> serverAddressResolverClass) {
			this.serverAddressResolverClass = serverAddressResolverClass;
		}

	}

	public static class TrustSettings {

		public enum Strategy {

			TRUST_ALL_CERTIFICATES,

			TRUST_CUSTOM_CA_SIGNED_CERTIFICATES,

			TRUST_SYSTEM_CA_SIGNED_CERTIFICATES

		}

		/**
		 * Configures the strategy to use use.
		 */
		private Strategy strategy = Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES;

		/**
		 * File of the certificate to use.
		 */
		private File certFile;

		/**
		 * Flag, if hostname verification is used.
		 */
		private boolean hostnameVerificationEnabled = false;

		public Strategy getStrategy() {
			return this.strategy;
		}

		public void setStrategy(Strategy strategy) {
			this.strategy = strategy;
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

	}

}
