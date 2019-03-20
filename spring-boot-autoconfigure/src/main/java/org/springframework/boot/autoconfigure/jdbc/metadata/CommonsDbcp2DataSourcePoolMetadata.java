/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc.metadata;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * {@link DataSourcePoolMetadata} for an Apache Commons DBCP2 {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 1.3.1
 */
public class CommonsDbcp2DataSourcePoolMetadata
		extends AbstractDataSourcePoolMetadata<BasicDataSource> {

	public CommonsDbcp2DataSourcePoolMetadata(BasicDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Integer getActive() {
		return getDataSource().getNumActive();
	}

	@Override
	public Integer getMax() {
		return getDataSource().getMaxTotal();
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
