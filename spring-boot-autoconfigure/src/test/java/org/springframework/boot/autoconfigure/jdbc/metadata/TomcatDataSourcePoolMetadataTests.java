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

package org.springframework.boot.autoconfigure.jdbc.metadata;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link TomcatDataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
public class TomcatDataSourcePoolMetadataTests extends
		AbstractDataSourcePoolMetadataTests<TomcatDataSourcePoolMetadata> {

	private TomcatDataSourcePoolMetadata dataSourceMetadata;

	@Before
	public void setup() {
		this.dataSourceMetadata = new TomcatDataSourcePoolMetadata(createDataSource(0, 2));
	}

	@Override
	protected TomcatDataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Override
	public void getValidationQuery() {
		DataSource dataSource = createDataSource(0, 4);
		dataSource.setValidationQuery("SELECT FROM FOO");
		assertEquals("SELECT FROM FOO",
				new TomcatDataSourcePoolMetadata(dataSource).getValidationQuery());
	}

	private DataSource createDataSource(int minSize, int maxSize) {
		DataSource dataSource = (DataSource) initializeBuilder().type(DataSource.class)
				.build();
		dataSource.setMinIdle(minSize);
		dataSource.setMaxActive(maxSize);

		// Avoid warnings
		dataSource.setInitialSize(minSize);
		dataSource.setMaxIdle(maxSize);
		return dataSource;
	}

}
