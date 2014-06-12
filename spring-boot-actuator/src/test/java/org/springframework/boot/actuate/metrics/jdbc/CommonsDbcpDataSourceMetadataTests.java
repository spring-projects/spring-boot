/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.jdbc;

import static org.junit.Assert.*;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Stephane Nicoll
 */
public class CommonsDbcpDataSourceMetadataTests extends AbstractDataSourceMetadataTests<CommonsDbcpDataSourceMetadata> {

	private CommonsDbcpDataSourceMetadata dataSourceMetadata;

	@Before
	public void setup() {
		this.dataSourceMetadata = createDataSourceMetadata(0, 2);
	}

	@Override
	protected CommonsDbcpDataSourceMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Test
	public void getPoolUsageWithNoCurrent() {
		CommonsDbcpDataSourceMetadata dsm = new CommonsDbcpDataSourceMetadata(createDataSource()) {
			@Override
			public Integer getPoolSize() {
				return null;
			}
		};
		assertNull(dsm.getPoolUsage());
	}

	@Test
	public void getPoolUsageWithNoMax() {
		CommonsDbcpDataSourceMetadata dsm = new CommonsDbcpDataSourceMetadata(createDataSource()) {
			@Override
			public Integer getMaxPoolSize() {
				return null;
			}
		};
		assertNull(dsm.getPoolUsage());
	}

	@Test
	public void getPoolUsageWithUnlimitedPool() {
		DataSourceMetadata unlimitedDataSource = createDataSourceMetadata(0, -1);
		assertEquals(Float.valueOf(-1F), unlimitedDataSource.getPoolUsage());
	}

	private CommonsDbcpDataSourceMetadata createDataSourceMetadata(int minSize, int maxSize) {
		BasicDataSource dataSource = createDataSource();
		dataSource.setMinIdle(minSize);
		dataSource.setMaxActive(maxSize);
		return new CommonsDbcpDataSourceMetadata(dataSource);
	}

	private BasicDataSource createDataSource() {
		return (BasicDataSource) initializeBuilder().type(BasicDataSource.class).build();
	}

}
