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

package org.springframework.boot.autoconfigure.jdbc;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.Before;

/**
 *
 * @author Stephane Nicoll
 */
public class TomcatDataSourceMetadataTests extends AbstractDataSourceMetadataTests<TomcatDataSourceMetadata> {

	private TomcatDataSourceMetadata dataSourceMetadata;

	@Before
	public void setup() {
		this.dataSourceMetadata = createDataSourceMetadata(0, 2);
	}

	@Override
	protected TomcatDataSourceMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	private TomcatDataSourceMetadata createDataSourceMetadata(int minSize, int maxSize) {
		DataSource dataSource = (DataSource) initializeBuilder().type(DataSource.class).build();
		dataSource.setMinIdle(minSize);
		dataSource.setMaxActive(maxSize);

		// Avoid warnings
		dataSource.setInitialSize(minSize);
		dataSource.setMaxIdle(maxSize);
		return new TomcatDataSourceMetadata(dataSource);
	}

}
