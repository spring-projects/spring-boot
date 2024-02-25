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

package org.springframework.boot.autoconfigure.r2dbc;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.r2dbc.spi.ValidationDepth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for R2DBC.
 *
 * @author Mark Paluch
 * @author Andreas Killaitis
 * @author Stephane Nicoll
 * @author Rodolpho S. Couto
 * @since 2.3.0
 */
@ConfigurationProperties(prefix = "spring.r2dbc")
public class R2dbcProperties {

	/**
	 * Database name. Set if no name is specified in the url. Default to "testdb" when
	 * using an embedded database.
	 */
	private String name;

	/**
	 * Whether to generate a random database name. Ignore any configured name when
	 * enabled.
	 */
	private boolean generateUniqueName;

	/**
	 * R2DBC URL of the database. database name, username, password and pooling options
	 * specified in the url take precedence over individual options.
	 */
	private String url;

	/**
	 * Login username of the database. Set if no username is specified in the url.
	 */
	private String username;

	/**
	 * Login password of the database. Set if no password is specified in the url.
	 */
	private String password;

	/**
	 * Additional R2DBC options.
	 */
	private final Map<String, String> properties = new LinkedHashMap<>();

	private final Pool pool = new Pool();

	private String uniqueName;

	/**
     * Returns the name of the R2dbcProperties.
     *
     * @return the name of the R2dbcProperties
     */
    public String getName() {
		return this.name;
	}

	/**
     * Sets the name property.
     *
     * @param name the new value for the name property
     */
    public void setName(String name) {
		this.name = name;
	}

	/**
     * Returns a boolean value indicating whether to generate a unique name.
     *
     * @return {@code true} if a unique name should be generated, {@code false} otherwise.
     */
    public boolean isGenerateUniqueName() {
		return this.generateUniqueName;
	}

	/**
     * Sets the flag indicating whether to generate a unique name for the property.
     * 
     * @param generateUniqueName the flag indicating whether to generate a unique name
     */
    public void setGenerateUniqueName(boolean generateUniqueName) {
		this.generateUniqueName = generateUniqueName;
	}

	/**
     * Returns the URL of the R2DBC connection.
     *
     * @return the URL of the R2DBC connection
     */
    public String getUrl() {
		return this.url;
	}

	/**
     * Sets the URL for the R2DBC connection.
     * 
     * @param url the URL to set
     */
    public void setUrl(String url) {
		this.url = url;
	}

	/**
     * Returns the username used for authentication when connecting to the database.
     *
     * @return the username used for authentication
     */
    public String getUsername() {
		return this.username;
	}

	/**
     * Sets the username for the R2DBC connection.
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
		this.username = username;
	}

	/**
     * Returns the password used for authentication.
     *
     * @return the password used for authentication
     */
    public String getPassword() {
		return this.password;
	}

	/**
     * Sets the password for the R2DBC connection.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the properties of the R2dbcProperties object.
     *
     * @return a Map containing the properties as key-value pairs
     */
    public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
     * Returns the pool associated with this R2dbcProperties instance.
     *
     * @return the pool associated with this R2dbcProperties instance
     */
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

	/**
     * Pool class.
     */
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
		private Duration maxLifeTime;

		/**
		 * Maximum time to acquire a connection from the pool. By default, wait
		 * indefinitely.
		 */
		private Duration maxAcquireTime;

		/**
		 * Maximum time to validate a connection from the pool. By default, wait
		 * indefinitely.
		 */
		private Duration maxValidationTime;

		/**
		 * Maximum time to wait to create a new connection. By default, wait indefinitely.
		 */
		private Duration maxCreateConnectionTime;

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
		private String validationQuery;

		/**
		 * Validation depth.
		 */
		private ValidationDepth validationDepth = ValidationDepth.LOCAL;

		/**
		 * Whether pooling is enabled. Requires r2dbc-pool.
		 */
		private boolean enabled = true;

		/**
         * Returns the minimum number of idle objects that should be maintained in the pool.
         *
         * @return the minimum number of idle objects
         */
        public int getMinIdle() {
			return this.minIdle;
		}

		/**
         * Sets the minimum number of idle objects in the pool.
         * 
         * @param minIdle the minimum number of idle objects to set
         */
        public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		/**
         * Returns the maximum idle time allowed for objects in the pool.
         *
         * @return the maximum idle time in Duration format
         */
        public Duration getMaxIdleTime() {
			return this.maxIdleTime;
		}

		/**
         * Sets the maximum idle time for the pool.
         * 
         * @param maxIdleTime the maximum idle time to be set
         */
        public void setMaxIdleTime(Duration maxIdleTime) {
			this.maxIdleTime = maxIdleTime;
		}

		/**
         * Returns the maximum life time of the pool.
         *
         * @return the maximum life time of the pool
         */
        public Duration getMaxLifeTime() {
			return this.maxLifeTime;
		}

		/**
         * Sets the maximum lifetime for objects in the pool.
         * 
         * @param maxLifeTime the maximum lifetime for objects in the pool
         */
        public void setMaxLifeTime(Duration maxLifeTime) {
			this.maxLifeTime = maxLifeTime;
		}

		/**
         * Returns the maximum validation time for the pool.
         *
         * @return the maximum validation time
         */
        public Duration getMaxValidationTime() {
			return this.maxValidationTime;
		}

		/**
         * Sets the maximum validation time for the pool.
         * 
         * @param maxValidationTime the maximum validation time to be set
         */
        public void setMaxValidationTime(Duration maxValidationTime) {
			this.maxValidationTime = maxValidationTime;
		}

		/**
         * Returns the maximum acquire time for the pool.
         *
         * @return the maximum acquire time
         */
        public Duration getMaxAcquireTime() {
			return this.maxAcquireTime;
		}

		/**
         * Sets the maximum time to acquire a resource from the pool.
         * 
         * @param maxAcquireTime the maximum time to acquire a resource, specified as a Duration object
         */
        public void setMaxAcquireTime(Duration maxAcquireTime) {
			this.maxAcquireTime = maxAcquireTime;
		}

		/**
         * Returns the maximum time allowed for creating a connection.
         *
         * @return the maximum time allowed for creating a connection
         */
        public Duration getMaxCreateConnectionTime() {
			return this.maxCreateConnectionTime;
		}

		/**
         * Sets the maximum time allowed for creating a connection.
         * 
         * @param maxCreateConnectionTime the maximum time allowed for creating a connection
         */
        public void setMaxCreateConnectionTime(Duration maxCreateConnectionTime) {
			this.maxCreateConnectionTime = maxCreateConnectionTime;
		}

		/**
         * Returns the initial size of the pool.
         *
         * @return the initial size of the pool
         */
        public int getInitialSize() {
			return this.initialSize;
		}

		/**
         * Sets the initial size of the pool.
         * 
         * @param initialSize the initial size of the pool
         */
        public void setInitialSize(int initialSize) {
			this.initialSize = initialSize;
		}

		/**
         * Returns the maximum size of the pool.
         *
         * @return the maximum size of the pool
         */
        public int getMaxSize() {
			return this.maxSize;
		}

		/**
         * Sets the maximum size of the pool.
         * 
         * @param maxSize the maximum size of the pool
         */
        public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

		/**
         * Returns the validation query used by the Pool class.
         *
         * @return the validation query used by the Pool class
         */
        public String getValidationQuery() {
			return this.validationQuery;
		}

		/**
         * Sets the validation query for the connection pool.
         * 
         * @param validationQuery the validation query to be set
         */
        public void setValidationQuery(String validationQuery) {
			this.validationQuery = validationQuery;
		}

		/**
         * Returns the validation depth of the Pool.
         * 
         * @return the validation depth of the Pool
         */
        public ValidationDepth getValidationDepth() {
			return this.validationDepth;
		}

		/**
         * Sets the validation depth for the pool.
         * 
         * @param validationDepth the validation depth to be set
         */
        public void setValidationDepth(ValidationDepth validationDepth) {
			this.validationDepth = validationDepth;
		}

		/**
         * Returns the current status of the Pool.
         * 
         * @return true if the Pool is enabled, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Pool.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
