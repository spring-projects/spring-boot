/*
 * Copyright 2012-2016 the original author or authors.
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

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Before;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HikariDataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
public class HikariDataSourcePoolMetadataTests
		extends AbstractDataSourcePoolMetadataTests<HikariDataSourcePoolMetadata> {

	private HikariDataSourcePoolMetadata dataSourceMetadata;

	@Before
	public void setup() {
		this.dataSourceMetadata = new HikariDataSourcePoolMetadata(
				createDataSource(0, 2));
	}

	@Override
	protected HikariDataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Override
	public void getValidationQuery() {
		HikariDataSource dataSource = createDataSource(0, 4);
		dataSource.setConnectionTestQuery("SELECT FROM FOO");
		assertThat(new HikariDataSourcePoolMetadata(dataSource).getValidationQuery())
				.isEqualTo("SELECT FROM FOO");
	}

	private HikariDataSource createDataSource(int minSize, int maxSize) {
		HikariDataSource dataSource = (HikariDataSource) initializeBuilder()
				.type(HikariDataSource.class).build();
		dataSource.setMinimumIdle(minSize);
		dataSource.setMaximumPoolSize(maxSize);
		return dataSource;
	}
}
