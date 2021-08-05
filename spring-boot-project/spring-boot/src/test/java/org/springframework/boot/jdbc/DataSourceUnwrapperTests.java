/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.jdbc;

import java.sql.SQLException;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.SmartDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link DataSourceUnwrapper}.
 *
 * @author Stephane Nicoll
 */
class DataSourceUnwrapperTests {

	@Test
	void unwrapWithTarget() {
		DataSource dataSource = new HikariDataSource();
		assertThat(DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class, HikariDataSource.class))
				.isSameAs(dataSource);
	}

	@Test
	void unwrapWithWrongTarget() {
		DataSource dataSource = new HikariDataSource();
		assertThat(DataSourceUnwrapper.unwrap(dataSource, SmartDataSource.class, SingleConnectionDataSource.class))
				.isNull();
	}

	@Test
	void unwrapWithDelegate() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInDelegate(wrapInDelegate(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
				.isSameAs(dataSource);
	}

	@Test
	void unwrapWithProxy() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
				.isSameAs(dataSource);
	}

	@Test
	void unwrapWithProxyAndDelegate() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInDelegate(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
				.isSameAs(dataSource);
	}

	@Test
	void unwrapWithSeveralLevelOfWrapping() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInDelegate(wrapInDelegate(wrapInProxy(wrapInDelegate(dataSource)))));
		assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
				.isSameAs(dataSource);
	}

	@Test
	void unwrapDataSourceProxy() {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
		DataSource actual = wrapInDelegate(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, PoolConfiguration.class, DataSourceProxy.class))
				.isSameAs(dataSource);
	}

	@Test
	void unwrappingIsNotAttemptedWhenTargetIsNotAnInterface() {
		DataSource dataSource = mock(DataSource.class);
		assertThat(DataSourceUnwrapper.unwrap(dataSource, HikariDataSource.class)).isNull();
		verifyNoMoreInteractions(dataSource);
	}

	@Test
	void unwrappingIsNotAttemptedWhenDataSourceIsNotWrapperForTarget() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		assertThat(DataSourceUnwrapper.unwrap(dataSource, Consumer.class)).isNull();
		verify(dataSource).isWrapperFor(Consumer.class);
		verifyNoMoreInteractions(dataSource);
	}

	private DataSource wrapInProxy(DataSource dataSource) {
		return (DataSource) new ProxyFactory(dataSource).getProxy();
	}

	private DataSource wrapInDelegate(DataSource dataSource) {
		return new DelegatingDataSource(dataSource);
	}

}
