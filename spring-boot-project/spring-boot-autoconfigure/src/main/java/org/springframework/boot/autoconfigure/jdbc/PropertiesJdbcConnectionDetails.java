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

package org.springframework.boot.autoconfigure.jdbc;

/**
 * Adapts {@link DataSourceProperties} to {@link JdbcConnectionDetails}.
 *
 * @author Andy Wilkinson
 */
final class PropertiesJdbcConnectionDetails implements JdbcConnectionDetails {

	private final DataSourceProperties properties;

	/**
     * Constructs a new PropertiesJdbcConnectionDetails object with the given DataSourceProperties.
     * 
     * @param properties the DataSourceProperties object containing the JDBC connection details
     */
    PropertiesJdbcConnectionDetails(DataSourceProperties properties) {
		this.properties = properties;
	}

	/**
     * Returns the username for the JDBC connection details.
     * 
     * @return the username determined by the properties
     */
    @Override
	public String getUsername() {
		return this.properties.determineUsername();
	}

	/**
     * Returns the password for the JDBC connection details.
     * 
     * @return the password for the JDBC connection details
     */
    @Override
	public String getPassword() {
		return this.properties.determinePassword();
	}

	/**
     * Returns the JDBC URL determined by the properties.
     *
     * @return the JDBC URL determined by the properties
     */
    @Override
	public String getJdbcUrl() {
		return this.properties.determineUrl();
	}

	/**
     * Returns the driver class name for the JDBC connection.
     * 
     * @return the driver class name
     */
    @Override
	public String getDriverClassName() {
		return this.properties.determineDriverClassName();
	}

	/**
     * Returns the XA data source class name.
     * 
     * @return the XA data source class name
     */
    @Override
	public String getXaDataSourceClassName() {
		return (this.properties.getXa().getDataSourceClassName() != null)
				? this.properties.getXa().getDataSourceClassName()
				: JdbcConnectionDetails.super.getXaDataSourceClassName();
	}

}
