/*
 * Copyright 2012-2020 the original author or authors.
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
