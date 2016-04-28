/*
 * Copyright 2012-2015 the original author or authors.
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

import javax.sql.DataSource;

/**
 * A base {@link DataSourcePoolMetadata} implementation.
 *
 * @param <T> the data source type
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public abstract class AbstractDataSourcePoolMetadata<T extends DataSource>
		implements DataSourcePoolMetadata {

	private final T dataSource;

	/**
	 * Create an instance with the data source to use.
	 * @param dataSource the data source
	 */
	protected AbstractDataSourcePoolMetadata(T dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Float getUsage() {
		Integer maxSize = getMax();
		Integer currentSize = getActive();
		if (maxSize == null || currentSize == null) {
			return null;
		}
		if (maxSize < 0) {
			return -1F;
		}
		if (currentSize == 0) {
			return 0F;
		}
		return (float) currentSize / (float) maxSize;
	}

	protected final T getDataSource() {
		return this.dataSource;
	}

}
