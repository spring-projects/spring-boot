/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.apache.tomcat.jdbc.pool.DataSource;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Post-processes beans of type {@link DataSource} and name 'dataSource' to apply the
 * values from {@link JdbcConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class TomcatJdbcConnectionDetailsBeanPostProcessor extends JdbcConnectionDetailsBeanPostProcessor<DataSource> {

	TomcatJdbcConnectionDetailsBeanPostProcessor(ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
		super(DataSource.class, connectionDetailsProvider);
	}

	@Override
	protected Object processDataSource(DataSource dataSource, JdbcConnectionDetails connectionDetails) {
		dataSource.setUrl(connectionDetails.getJdbcUrl());
		dataSource.setUsername(connectionDetails.getUsername());
		dataSource.setPassword(connectionDetails.getPassword());
		dataSource.setDriverClassName(connectionDetails.getDriverClassName());
		return dataSource;
	}

}
