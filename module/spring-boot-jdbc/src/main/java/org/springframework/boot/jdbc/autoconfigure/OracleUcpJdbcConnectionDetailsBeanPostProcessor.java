/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

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

	OracleUcpJdbcConnectionDetailsBeanPostProcessor(ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
		super(PoolDataSourceImpl.class, connectionDetailsProvider);
	}

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
