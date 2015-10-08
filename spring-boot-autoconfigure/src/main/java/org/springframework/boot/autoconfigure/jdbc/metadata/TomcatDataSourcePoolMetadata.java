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

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;

/**
 * {@link DataSourcePoolMetadata} for a Tomcat DataSource.
 *
 * @author Stephane Nicoll
 */
public class TomcatDataSourcePoolMetadata
		extends AbstractDataSourcePoolMetadata<DataSource> {

	public TomcatDataSourcePoolMetadata(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Integer getActive() {
		ConnectionPool pool = getDataSource().getPool();
		return (pool == null ? 0 : pool.getActive());
	}

	@Override
	public Integer getMax() {
		return getDataSource().getMaxActive();
	}

	@Override
	public Integer getMin() {
		return getDataSource().getMinIdle();
	}

	@Override
	public String getValidationQuery() {
		return getDataSource().getValidationQuery();
	}

}
