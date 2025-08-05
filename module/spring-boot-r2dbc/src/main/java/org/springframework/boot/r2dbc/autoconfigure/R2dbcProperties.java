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

package org.springframework.boot.r2dbc.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.r2dbc.spi.ValidationDepth;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for R2DBC.
 *
 * @author Mark Paluch
 * @author Andreas Killaitis
 * @author Stephane Nicoll
 * @author Rodolpho S. Couto
 * @since 4.0.0
 */
@ConfigurationProperties("spring.r2dbc")
public class R2dbcProperties {

	/**
	 * Database name. Set if no name is specified in the url. Default to "testdb" when
	 * using an embedded database.
	 */
	private @Nullable String name;

	/**
	 * Whether to generate a random database name. Ignore any configured name when
	 * enabled.
	 */
	private boolean generateUniqueName;

	/**
	 * R2DBC URL of the database. database name, username, password and pooling options
	 * specified in the url take precedence over individual options.
	 */
	private @Nullable String url;

	/**
	 * Login username of the database. Set if no username is specified in the url.
	 */
	private @Nullable String username;

	/**
	 * Login password of the database. Set if no password is specified in the url.
	 */
	private @Nullable String password;

	/**
	 * Additional R2DBC options.
	 */
	private final Map<String, String> properties = new LinkedHashMap<>();

	private final Pool pool = new Pool();

	private @Nullable String uniqueName;

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	public boolean isGenerateUniqueName() {
		return this.generateUniqueName;
	}

	public void setGenerateUniqueName(boolean generateUniqueName) {
		this.generateUniqueName = generateUniqueName;
	}

	public @Nullable String getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
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

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public Pool getPool() {
		return this.pool;
	}

	/**
	 * Provide a unique name specific to this instance. Calling this method several times
	 * return the same unique name.
	 * @return a unique name for this instance
	 */
	public String determineUniqueName() {
		if (this.uniqueName == null) {
			this.uniqueName = UUID.randomUUID().toString();
		}
		return this.uniqueName;
	}

	public static class Pool {

		/**
		 * Minimal number of idle connections.
		 */
		private int minIdle = 0;

		/**
		 * Maximum amount of time that a connection is allowed to sit idle in the pool.
		 */
		private Duration maxIdleTime = Duration.ofMinutes(30);

		/**
		 * Maximum lifetime of a connection in the pool. By default, connections have an
		 * infinite lifetime.
		 */
		private @Nullable Duration maxLifeTime;

		/**
		 * Maximum time to acquire a connection from the pool. By default, wait
		 * indefinitely.
		 */
		private @Nullable Duration maxAcquireTime;

		/**
		 * Number of acquire retries if the first acquire attempt fails.
		 */
		private int acquireRetry = 1;

		/**
		 * Maximum time to validate a connection from the pool. By default, wait
		 * indefinitely.
		 */
		private @Nullable Duration maxValidationTime;

		/**
		 * Maximum time to wait to create a new connection. By default, wait indefinitely.
		 */
		private @Nullable Duration maxCreateConnectionTime;

		/**
		 * Initial connection pool size.
		 */
		private int initialSize = 10;

		/**
		 * Maximal connection pool size.
		 */
		private int maxSize = 10;

		/**
		 * Validation query.
		 */
		private @Nullable String validationQuery;

		/**
		 * Validation depth.
		 */
		private ValidationDepth validationDepth = ValidationDepth.LOCAL;

		/**
		 * Whether pooling is enabled. Requires r2dbc-pool.
		 */
		private boolean enabled = true;

		public int getMinIdle() {
			return this.minIdle;
		}

		public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		public Duration getMaxIdleTime() {
			return this.maxIdleTime;
		}

		public void setMaxIdleTime(Duration maxIdleTime) {
			this.maxIdleTime = maxIdleTime;
		}

		public @Nullable Duration getMaxLifeTime() {
			return this.maxLifeTime;
		}

		public void setMaxLifeTime(@Nullable Duration maxLifeTime) {
			this.maxLifeTime = maxLifeTime;
		}

		public @Nullable Duration getMaxValidationTime() {
			return this.maxValidationTime;
		}

		public void setMaxValidationTime(@Nullable Duration maxValidationTime) {
			this.maxValidationTime = maxValidationTime;
		}

		public @Nullable Duration getMaxAcquireTime() {
			return this.maxAcquireTime;
		}

		public void setMaxAcquireTime(@Nullable Duration maxAcquireTime) {
			this.maxAcquireTime = maxAcquireTime;
		}

		public int getAcquireRetry() {
			return this.acquireRetry;
		}

		public void setAcquireRetry(int acquireRetry) {
			this.acquireRetry = acquireRetry;
		}

		public @Nullable Duration getMaxCreateConnectionTime() {
			return this.maxCreateConnectionTime;
		}

		public void setMaxCreateConnectionTime(@Nullable Duration maxCreateConnectionTime) {
			this.maxCreateConnectionTime = maxCreateConnectionTime;
		}

		public int getInitialSize() {
			return this.initialSize;
		}

		public void setInitialSize(int initialSize) {
			this.initialSize = initialSize;
		}

		public int getMaxSize() {
			return this.maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

		public @Nullable String getValidationQuery() {
			return this.validationQuery;
		}

		public void setValidationQuery(@Nullable String validationQuery) {
			this.validationQuery = validationQuery;
		}

		public ValidationDepth getValidationDepth() {
			return this.validationDepth;
		}

		public void setValidationDepth(ValidationDepth validationDepth) {
			this.validationDepth = validationDepth;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
