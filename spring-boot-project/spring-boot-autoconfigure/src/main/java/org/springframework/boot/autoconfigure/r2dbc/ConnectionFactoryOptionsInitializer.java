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

import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.util.StringUtils;

/**
 * Initialize a {@link Builder} based on {@link R2dbcProperties}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConnectionFactoryOptionsInitializer {

	/**
	 * Initialize a {@link Builder ConnectionFactoryOptions.Builder} using the specified
	 * properties.
	 * @param properties the properties to use to initialize the builder
	 * @param connectionDetails the connection details to use to initialize the builder
	 * @param embeddedDatabaseConnection the embedded connection to use as a fallback
	 * @return an initialized builder
	 * @throws ConnectionFactoryBeanCreationException if no suitable connection could be
	 * determined
	 */
	ConnectionFactoryOptions.Builder initialize(R2dbcProperties properties, R2dbcConnectionDetails connectionDetails,
			Supplier<EmbeddedDatabaseConnection> embeddedDatabaseConnection) {
		if (connectionDetails != null) {
			return connectionDetails.getConnectionFactoryOptions().mutate();
		}
		EmbeddedDatabaseConnection embeddedConnection = embeddedDatabaseConnection.get();
		if (embeddedConnection != EmbeddedDatabaseConnection.NONE) {
			return initializeEmbeddedOptions(properties, embeddedConnection);
		}
		throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL", null,
				embeddedConnection);
	}

	/**
     * Initializes the embedded options for the R2DBC connection factory.
     * 
     * @param properties the R2DBC properties
     * @param embeddedDatabaseConnection the embedded database connection
     * @return the builder with the initialized options
     * @throws ConnectionFactoryBeanCreationException if failed to determine a suitable R2DBC Connection URL
     */
    private Builder initializeEmbeddedOptions(R2dbcProperties properties,
			EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		String url = embeddedDatabaseConnection.getUrl(determineEmbeddedDatabaseName(properties));
		if (url == null) {
			throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL", url,
					embeddedDatabaseConnection);
		}
		Builder builder = ConnectionFactoryOptions.parse(url).mutate();
		String username = determineEmbeddedUsername(properties);
		if (StringUtils.hasText(username)) {
			builder.option(ConnectionFactoryOptions.USER, username);
		}
		if (StringUtils.hasText(properties.getPassword())) {
			builder.option(ConnectionFactoryOptions.PASSWORD, properties.getPassword());
		}
		return builder;
	}

	/**
     * Determines the name of the embedded database based on the provided R2dbcProperties.
     * If a database name is specified in the properties, that name is returned.
     * Otherwise, the default name "testdb" is returned.
     *
     * @param properties the R2dbcProperties object containing the database configuration
     * @return the name of the embedded database
     */
    private String determineEmbeddedDatabaseName(R2dbcProperties properties) {
		String databaseName = determineDatabaseName(properties);
		return (databaseName != null) ? databaseName : "testdb";
	}

	/**
     * Determines the database name based on the provided R2dbcProperties.
     * If the generateUniqueName flag is set to true, it will generate a unique name.
     * If a name is provided in the properties, it will use that name.
     * If neither condition is met, it will return null.
     *
     * @param properties the R2dbcProperties object containing the database properties
     * @return the determined database name or null if no name is determined
     */
    private String determineDatabaseName(R2dbcProperties properties) {
		if (properties.isGenerateUniqueName()) {
			return properties.determineUniqueName();
		}
		if (StringUtils.hasLength(properties.getName())) {
			return properties.getName();
		}
		return null;
	}

	/**
     * Determines the embedded username based on the provided R2dbcProperties.
     * If the username is not empty or null, it is returned.
     * Otherwise, the default username "sa" is returned.
     *
     * @param properties the R2dbcProperties object containing the username
     * @return the determined embedded username
     */
    private String determineEmbeddedUsername(R2dbcProperties properties) {
		String username = ifHasText(properties.getUsername());
		return (username != null) ? username : "sa";
	}

	/**
     * Creates a new ConnectionFactoryBeanCreationException with the specified message, R2DBC URL, and embedded database connection.
     *
     * @param message the detail message
     * @param r2dbcUrl the R2DBC URL
     * @param embeddedDatabaseConnection the embedded database connection
     * @return the new ConnectionFactoryBeanCreationException
     */
    private ConnectionFactoryBeanCreationException connectionFactoryBeanCreationException(String message,
			String r2dbcUrl, EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		return new ConnectionFactoryBeanCreationException(message, r2dbcUrl, embeddedDatabaseConnection);
	}

	/**
     * Checks if the given candidate string has text.
     * 
     * @param candidate the string to be checked
     * @return the candidate string if it has text, otherwise null
     */
    private String ifHasText(String candidate) {
		return (StringUtils.hasText(candidate)) ? candidate : null;
	}

	/**
     * ConnectionFactoryBeanCreationException class.
     */
    static class ConnectionFactoryBeanCreationException extends BeanCreationException {

		private final String url;

		private final EmbeddedDatabaseConnection embeddedDatabaseConnection;

		/**
         * Constructs a new ConnectionFactoryBeanCreationException with the specified detail message, URL, and embedded database connection.
         * 
         * @param message the detail message for the exception
         * @param url the URL used for the connection
         * @param embeddedDatabaseConnection the embedded database connection used
         */
        ConnectionFactoryBeanCreationException(String message, String url,
				EmbeddedDatabaseConnection embeddedDatabaseConnection) {
			super(message);
			this.url = url;
			this.embeddedDatabaseConnection = embeddedDatabaseConnection;
		}

		/**
         * Returns the URL associated with this ConnectionFactoryBeanCreationException.
         *
         * @return the URL associated with this ConnectionFactoryBeanCreationException
         */
        String getUrl() {
			return this.url;
		}

		/**
         * Returns the embedded database connection.
         *
         * @return the embedded database connection
         */
        EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
			return this.embeddedDatabaseConnection;
		}

	}

}
