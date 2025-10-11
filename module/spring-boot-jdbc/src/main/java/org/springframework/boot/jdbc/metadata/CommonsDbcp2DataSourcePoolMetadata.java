/*
 * Copyright 2012-present the original author or authors.
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

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;

/**
 * {@link DataSourcePoolMetadata} for an Apache Commons DBCP2 {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CommonsDbcp2DataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<BasicDataSource> {

	public CommonsDbcp2DataSourcePoolMetadata(BasicDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public @Nullable Integer getActive() {
		return getDataSource().getNumActive();
	}

	@Override
	public @Nullable Integer getIdle() {
		return getDataSource().getNumIdle();
	}

	@Override
	public @Nullable Integer getMax() {
		return getDataSource().getMaxTotal();
	}

	@Override
	public @Nullable Integer getMin() {
		return getDataSource().getMinIdle();
	}

	@Override
	public @Nullable String getValidationQuery() {
		return getDataSource().getValidationQuery();
	}

	@Override
	public @Nullable Boolean getDefaultAutoCommit() {
		return getDataSource().getDefaultAutoCommit();
	}

}
