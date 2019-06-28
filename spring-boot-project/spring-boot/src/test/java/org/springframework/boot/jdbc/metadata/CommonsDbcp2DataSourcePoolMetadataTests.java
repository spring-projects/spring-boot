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

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonsDbcp2DataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
class CommonsDbcp2DataSourcePoolMetadataTests
		extends AbstractDataSourcePoolMetadataTests<CommonsDbcp2DataSourcePoolMetadata> {

	private final CommonsDbcp2DataSourcePoolMetadata dataSourceMetadata = createDataSourceMetadata(0, 2);

	@Override
	protected CommonsDbcp2DataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Test
	void getPoolUsageWithNoCurrent() {
		CommonsDbcp2DataSourcePoolMetadata dsm = new CommonsDbcp2DataSourcePoolMetadata(createDataSource()) {
			@Override
			public Integer getActive() {
				return null;
			}
		};
		assertThat(dsm.getUsage()).isNull();
	}

	@Test
	void getPoolUsageWithNoMax() {
		CommonsDbcp2DataSourcePoolMetadata dsm = new CommonsDbcp2DataSourcePoolMetadata(createDataSource()) {
			@Override
			public Integer getMax() {
				return null;
			}
		};
		assertThat(dsm.getUsage()).isNull();
	}

	@Test
	void getPoolUsageWithUnlimitedPool() {
		DataSourcePoolMetadata unlimitedDataSource = createDataSourceMetadata(0, -1);
		assertThat(unlimitedDataSource.getUsage()).isEqualTo(Float.valueOf(-1F));
	}

	@Override
	public void getValidationQuery() {
		BasicDataSource dataSource = createDataSource();
		dataSource.setValidationQuery("SELECT FROM FOO");
		assertThat(new CommonsDbcp2DataSourcePoolMetadata(dataSource).getValidationQuery())
				.isEqualTo("SELECT FROM FOO");
	}

	@Override
	public void getDefaultAutoCommit() {
		BasicDataSource dataSource = createDataSource();
		dataSource.setDefaultAutoCommit(false);
		assertThat(new CommonsDbcp2DataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
	}

	private CommonsDbcp2DataSourcePoolMetadata createDataSourceMetadata(int minSize, int maxSize) {
		BasicDataSource dataSource = createDataSource();
		dataSource.setMinIdle(minSize);
		dataSource.setMaxTotal(maxSize);
		return new CommonsDbcp2DataSourcePoolMetadata(dataSource);
	}

	private BasicDataSource createDataSource() {
		return initializeBuilder().type(BasicDataSource.class).build();
	}

}
