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

import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSourceImpl;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Post-processes beans of type {@link PoolDataSourceImpl} and name 'dataSource' to apply
 * the values from {@link JdbcConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class OracleUcpJdbcConnectionDetailsBeanPostProcessor
		extends JdbcConnectionDetailsBeanPostProcessor<PoolDataSourceImpl> {

	/**
     * Constructs a new OracleUcpJdbcConnectionDetailsBeanPostProcessor with the specified connectionDetailsProvider.
     * 
     * @param connectionDetailsProvider the provider for the JdbcConnectionDetails
     */
    OracleUcpJdbcConnectionDetailsBeanPostProcessor(ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
		super(PoolDataSourceImpl.class, connectionDetailsProvider);
	}

	/**
     * Sets the URL, username, password, and connection factory class name of the given data source.
     * 
     * @param dataSource the data source to be processed
     * @param connectionDetails the JDBC connection details containing the URL, username, password, and driver class name
     * @return the processed data source
     * @throws RuntimeException if failed to set the URL, username, or password of the data source
     */
    @Override
	protected Object processDataSource(PoolDataSourceImpl dataSource, JdbcConnectionDetails connectionDetails) {
		try {
			dataSource.setURL(connectionDetails.getJdbcUrl());
			dataSource.setUser(connectionDetails.getUsername());
			dataSource.setPassword(connectionDetails.getPassword());
			dataSource.setConnectionFactoryClassName(connectionDetails.getDriverClassName());
			return dataSource;
		}
		catch (SQLException ex) {
			throw new RuntimeException("Failed to set URL / user / password of datasource", ex);
		}
	}

}
