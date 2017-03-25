/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Collections;

import javax.sql.DataSource;

import com.p6spy.engine.spy.P6DataSource;
import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.CustomDataSourceProxy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProxyDataSourcePoolMetadataProvider}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.5.3
 */
public class ProxyDataSourcePoolMetadataProviderTests {

	private ProxyDataSourcePoolMetadataProvider proxyDataSourcePoolMetadataProvider;

	@Before
	public void before() throws Exception {
		this.proxyDataSourcePoolMetadataProvider = new ProxyDataSourcePoolMetadataProvider();
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForHikari() {
		DataSource dataSource = createDataSource(HikariDataSource.class);
		DataSource proxyDataSource = new P6DataSource(dataSource);
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(proxyDataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(HikariDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForTomcat() {
		DataSource dataSource = createDataSource(org.apache.tomcat.jdbc.pool.DataSource.class);
		DataSource proxyDataSource = new P6DataSource(dataSource);
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(proxyDataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(TomcatDataSourcePoolMetadata.class);
	}

	@Test
	@Deprecated
	public void testReturnDataSourcePoolMetadataProviderForDbcp() {
		DataSource dataSource = createDataSource(org.apache.commons.dbcp.BasicDataSource.class);
		DataSource proxyDataSource = new P6DataSource(dataSource);
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(proxyDataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcpDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForDbcp2() {
		DataSource dataSource = createDataSource(org.apache.commons.dbcp2.BasicDataSource.class);
		DataSource proxyDataSource = new P6DataSource(dataSource);
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(proxyDataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcp2DataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderFromMultipleProxies() {
		DataSource dataSource = createDataSource(HikariDataSource.class);
		DataSource proxyDataSource = new ProxyDataSource(new P6DataSource(dataSource));
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(proxyDataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(HikariDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnNullForNonProxy() {
		DataSource dataSource = createDataSource(HikariDataSource.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isNull();
	}

	@Test
	public void testReturnNullForUnknownProxyDataSource() {
		DataSource dataSource = createDataSource(HikariDataSource.class);
		DataSource proxyDataSource = new CustomDataSourceProxy(dataSource);
		DataSourcePoolMetadata dataSourcePoolMetadata = this.proxyDataSourcePoolMetadataProvider.getDataSourcePoolMetadata(proxyDataSource);
		assertThat(dataSourcePoolMetadata).isNull();
	}

	private DataSource createDataSource(Class<? extends DataSource> type) {
		return DataSourceBuilder.create()
				.driverClassName("org.hsqldb.jdbc.JDBCDriver")
				.url("jdbc:hsqldb:mem:test").username("sa")
				.type(type)
				.proxyTypes(Collections.<Class<? extends DataSource>>emptyList())
				.build();
	}
}
