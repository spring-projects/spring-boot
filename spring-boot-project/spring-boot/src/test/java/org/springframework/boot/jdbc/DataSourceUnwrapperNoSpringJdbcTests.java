/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataSourceUnwrapper} when spring-jdbc is not available.
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-jdbc-*.jar")
public class DataSourceUnwrapperNoSpringJdbcTests {

	@Test
	public void unwrapWithProxy() {
		DataSource dataSource = new HikariDataSource();
		DataSource actual = wrapInProxy(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, HikariDataSource.class)).isSameAs(dataSource);
	}

	@Test
	public void unwrapDataSourceProxy() {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
		DataSource actual = wrapInProxy(wrapInProxy(dataSource));
		assertThat(DataSourceUnwrapper.unwrap(actual, DataSourceProxy.class)).isSameAs(dataSource);
	}

	private DataSource wrapInProxy(DataSource dataSource) {
		return (DataSource) new ProxyFactory(dataSource).getProxy();
	}

}
