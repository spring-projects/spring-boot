/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import java.sql.SQLException;

import oracle.ucp.config.PoolPropertiesException;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.OpaqueString;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OracleUcpJdbcConnectionDetailsBeanPostProcessor}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class OracleUcpJdbcConnectionDetailsBeanPostProcessorTests {

	@Test
	@SuppressWarnings("unchecked")
	void setUsernamePasswordUrlAndDriverClassName() throws SQLException {
		PoolDataSourceImpl dataSource = new PoolDataSourceImpl();
		dataSource.setURL("will-be-overwritten");
		dataSource.setUser("will-be-overwritten");
		dataSource.setPassword("will-be-overwritten");
		dataSource.setConnectionFactoryClassName("will-be-overwritten");
		new OracleUcpJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
				new TestJdbcConnectionDetails());
		assertThat(dataSource.getURL()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
		assertThat(dataSource.getUser()).isEqualTo("user-1");
		assertThat(dataSource).extracting(this::getPassword).isEqualTo("password-1");
		assertThat(dataSource.getConnectionFactoryClassName())
			.isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
	}

	private String getPassword(PoolDataSource target) {
		try {
			return ((OpaqueString) target.getPoolProperties().getPoolProperty("password")).get();
		}
		catch (PoolPropertiesException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
