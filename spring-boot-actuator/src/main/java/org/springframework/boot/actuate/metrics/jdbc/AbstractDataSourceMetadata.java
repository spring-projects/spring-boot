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

import javax.sql.DataSource;

/**
 * A base {@link DataSourceMetadata} implementation.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public abstract class AbstractDataSourceMetadata<D extends DataSource> implements DataSourceMetadata {

	private final D dataSource;

	/**
	 * Create an instance with the data source to use.
	 */
	protected AbstractDataSourceMetadata(D dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Float getPoolUsage() {
		Integer max = getMaxPoolSize();
		if (max == null) {
			return null;
		}
		if (max < 0) {
			return -1F;
		}
		Integer current = getPoolSize();
		if (current == null) {
			return null;
		}
		if (current == 0) {
			return 0F;
		}
		return (float) current / max; // something like that
	}

	protected final D getDataSource() {
		return dataSource;
	}

}
