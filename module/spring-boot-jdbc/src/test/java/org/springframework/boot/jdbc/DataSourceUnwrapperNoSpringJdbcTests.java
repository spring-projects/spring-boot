/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataSourceUnwrapper} when spring-jdbc is not available.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("spring-jdbc-*.jar")
class DataSourceUnwrapperNoSpringJdbcTests {

	@Test
	void unwrapWithProxy() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, HikariConfigMXBean.class, HikariDataSource.class))
			.isSameAs(dataSource);
	}

	@Test
	void unwrapDataSourceProxy() {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
		DataSource actual = wrapInProxy(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, PoolConfiguration.class, DataSourceProxy.class))
			.isSameAs(dataSource);
	}

	private DataSource wrapInProxy(DataSource dataSource) {
		return (DataSource) new ProxyFactory(dataSource).getProxy();
	}

}
