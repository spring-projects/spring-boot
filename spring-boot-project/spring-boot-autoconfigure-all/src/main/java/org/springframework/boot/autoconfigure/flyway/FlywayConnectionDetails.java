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

package org.springframework.boot.autoconfigure.flyway;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.jdbc.DatabaseDriver;

/**
 * Details required for Flyway to establish a connection to an SQL service using JDBC.
 *
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface FlywayConnectionDetails extends ConnectionDetails {

	/**
	 * Username for the database or {@code null} if no Flyway-specific configuration is
	 * required.
	 * @return the username for the database or {@code null}
	 */
	String getUsername();

	/**
	 * Password for the database or {@code null} if no Flyway-specific configuration is
	 * required.
	 * @return the password for the database or {@code null}
	 */
	String getPassword();

	/**
	 * JDBC URL for the database or {@code null} if no Flyway-specific configuration is
	 * required.
	 * @return the JDBC URL for the database or {@code null}
	 */
	String getJdbcUrl();

	/**
	 * The name of the JDBC driver class. Defaults to the class name of the driver
	 * specified in the JDBC URL or {@code null} when no JDBC URL is configured.
	 * @return the JDBC driver class name or {@code null}
	 * @see #getJdbcUrl()
	 * @see DatabaseDriver#fromJdbcUrl(String)
	 * @see DatabaseDriver#getDriverClassName()
	 */
	default String getDriverClassName() {
		String jdbcUrl = getJdbcUrl();
		return (jdbcUrl != null) ? DatabaseDriver.fromJdbcUrl(jdbcUrl).getDriverClassName() : null;
	}

}
