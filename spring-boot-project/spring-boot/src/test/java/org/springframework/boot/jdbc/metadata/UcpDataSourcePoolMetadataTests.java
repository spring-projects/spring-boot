/*
 * Copyright 2012-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;

/**
 * Tests for {@link UcpDataSourcePoolMetadata}.
 *
 * @author Fabio Grassi
 */
public class UcpDataSourcePoolMetadataTests extends AbstractDataSourcePoolMetadataTests<UcpDataSourcePoolMetadata> {

	private final UcpDataSourcePoolMetadata dataSourceMetadata = new UcpDataSourcePoolMetadata(createDataSource(0, 2));

	@Override
	protected UcpDataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Override
	public void getValidationQuery() {
		PoolDataSource dataSource = createDataSource(0, 4);
		try {
			dataSource.setSQLForValidateConnection("SELECT NULL FROM DUAL");
		} catch (SQLException se) {
			throw new InvalidDataAccessApiUsageException("Error while setting property SQLForValidateConnection", se);
		}
		assertThat(new UcpDataSourcePoolMetadata(dataSource).getValidationQuery()).isEqualTo("SELECT NULL FROM DUAL");
	}

	@Override
	public void getDefaultAutoCommit() {
		PoolDataSource dataSource = createDataSource(0, 4);
		try {
			dataSource.setConnectionProperty("autoCommit", "false");
		} catch (SQLException se) {
			throw new InvalidDataAccessApiUsageException("Error while setting property connectionProperties.autoCommit", se);
		}
		assertThat(new UcpDataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
	}

	private PoolDataSource createDataSource(int minSize, int maxSize) {
		PoolDataSource dataSource = initializeBuilder().type(PoolDataSourceImpl.class).build();
		try {
			dataSource.setInitialPoolSize(minSize);
			dataSource.setMinPoolSize(minSize);
			dataSource.setMaxPoolSize(maxSize);
		} catch (SQLException se) {
			throw new InvalidDataAccessApiUsageException("Error while setting a property", se);
		}
		return dataSource;
	}
	
}
