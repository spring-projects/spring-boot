/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.SmartDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

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
	void unwrapWithLazyConnectionDataSource() {
		HikariDataSource dataSource = new HikariDataSource();
		DataSource actual = new LazyConnectionDataSourceProxy(dataSource);
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
	void unwrapRootWithPlainDataSource() {
		DataSource dataSource = new HikariDataSource();
		assertThat(DataSourceUnwrapper.unwrapRoot(dataSource)).isSameAs(dataSource);
	}

	@Test
	void unwrapRootWithDelegate() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInDelegate(wrapInDelegate(dataSource));
		assertThat(DataSourceUnwrapper.unwrapRoot(actual)).isSameAs(dataSource);
	}

	@Test
	void unwrapRootWithProxy() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrapRoot(actual)).isSameAs(dataSource);
	}

	@Test
	void unwrapRootWithLazyConnectionDataSource() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = new LazyConnectionDataSourceProxy(dataSource);
		assertThat(DataSourceUnwrapper.unwrapRoot(actual)).isSameAs(dataSource);
	}

	@Test
	void unwrapRootWithSeveralLevelOfWrapping() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInDelegate(wrapInDelegate(wrapInProxy(wrapInDelegate(dataSource)))));
		assertThat(DataSourceUnwrapper.unwrapRoot(actual)).isSameAs(dataSource);
	}

	@Test
	void unwrapRootWithSelfReturningWrapper() {
		DataSource dataSource = new SelfReturningDataSource();
		assertThat(DataSourceUnwrapper.unwrapRoot(dataSource)).isSameAs(dataSource);
	}

	@Test
	void unwrapRootWithDelegateWrappingSelfReturningWrapper() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = new RootAwareDelegatingDataSource(new SelfReturningDataSource(), dataSource);
		assertThat(DataSourceUnwrapper.unwrapRoot(actual)).isSameAs(dataSource);
	}

	@Test
	void unwrappingIsNotAttemptedWhenTargetIsNotAnInterface() {
		DataSource dataSource = mock(DataSource.class);
		assertThat(DataSourceUnwrapper.unwrap(dataSource, HikariDataSource.class)).isNull();
		then(dataSource).shouldHaveNoMoreInteractions();
	}

	@Test
	void unwrappingIsNotAttemptedWhenDataSourceIsNotWrapperForTarget() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		assertThat(DataSourceUnwrapper.unwrap(dataSource, Consumer.class)).isNull();
		then(dataSource).should().isWrapperFor(Consumer.class);
		then(dataSource).shouldHaveNoMoreInteractions();
	}

	private DataSource wrapInProxy(DataSource dataSource) {
		return (DataSource) new ProxyFactory(dataSource).getProxy();
	}

	private DataSource wrapInDelegate(DataSource dataSource) {
		return new DelegatingDataSource(dataSource);
	}

	private static final class SelfReturningDataSource extends AbstractDataSource {

		@Override
		public Connection getConnection() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Connection getConnection(String username, String password) {
			throw new UnsupportedOperationException();
		}

	}

	private static final class RootAwareDelegatingDataSource extends DelegatingDataSource {

		private final DataSource root;

		private RootAwareDelegatingDataSource(DataSource target, DataSource root) {
			super(target);
			this.root = root;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return (iface.isInstance(this.root)) ? iface.cast(this.root) : super.unwrap(iface);
		}

	}

}
