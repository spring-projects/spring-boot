/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Post-processes beans of type {@link HikariDataSource} and name 'dataSource' to apply
 * the values from {@link JdbcConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class HikariJdbcConnectionDetailsBeanPostProcessor extends JdbcConnectionDetailsBeanPostProcessor<HikariDataSource> {

	HikariJdbcConnectionDetailsBeanPostProcessor(ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
		super(HikariDataSource.class, connectionDetailsProvider);
	}

	@Override
	protected Object processDataSource(HikariDataSource dataSource, JdbcConnectionDetails connectionDetails) {
		dataSource.setJdbcUrl(connectionDetails.getJdbcUrl());
		dataSource.setUsername(connectionDetails.getUsername());
		dataSource.setPassword(connectionDetails.getPassword());
		String driverClassName = connectionDetails.getDriverClassName();
		if (driverClassName != null) {
			dataSource.setDriverClassName(driverClassName);
		}
		return dataSource;
	}

}
