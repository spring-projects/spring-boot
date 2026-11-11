/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OracleUcpDataSourcePoolMetadata}.
 *
 * @author Fabio Grassi
 */
class OracleUcpDataSourcePoolMetadataTests
		extends AbstractDataSourcePoolMetadataTests<OracleUcpDataSourcePoolMetadata> {

	private final OracleUcpDataSourcePoolMetadata dataSourceMetadata = new OracleUcpDataSourcePoolMetadata(
			createDataSource(0, 2));

	@Override
	protected OracleUcpDataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Override
	void getValidationQuery() throws SQLException {
		PoolDataSource dataSource = createDataSource(0, 4);
		dataSource.setSQLForValidateConnection("SELECT NULL FROM DUAL");
		assertThat(new OracleUcpDataSourcePoolMetadata(dataSource).getValidationQuery())
			.isEqualTo("SELECT NULL FROM DUAL");
	}

	@Override
	void getDefaultAutoCommit() throws SQLException {
		PoolDataSource dataSource = createDataSource(0, 4);
		dataSource.setConnectionProperty("autoCommit", "false");
		assertThat(new OracleUcpDataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
	}

	private PoolDataSource createDataSource(int minSize, int maxSize) {
		try {
			PoolDataSource dataSource = initializeBuilder().type(PoolDataSourceImpl.class).build();
			dataSource.setInitialPoolSize(minSize);
			dataSource.setMinPoolSize(minSize);
			dataSource.setMaxPoolSize(maxSize);
			return dataSource;
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Error while configuring PoolDataSource", ex);
		}
	}

}
