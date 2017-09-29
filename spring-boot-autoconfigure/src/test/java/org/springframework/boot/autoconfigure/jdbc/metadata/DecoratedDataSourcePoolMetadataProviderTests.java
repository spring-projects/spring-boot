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

import java.util.Random;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.decorator.DecoratedDataSource;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DecoratedDataSourcePoolMetadataProvider}.
 *
 * @author Arthur Gavlyukovskiy
 */
public class DecoratedDataSourcePoolMetadataProviderTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.initialize:false",
			"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void after() {
		this.context.close();
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForHikari() {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.type:" + HikariDataSource.class.getName());
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = this.context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(HikariDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForTomcat() {
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = this.context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(TomcatDataSourcePoolMetadata.class);
	}

	@Test
	@Deprecated
	public void testReturnDataSourcePoolMetadataProviderForDbcp() {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.type:" + org.apache.commons.dbcp.BasicDataSource.class.getName());
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = this.context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcpDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForDbcp2() {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.type:" + org.apache.commons.dbcp2.BasicDataSource.class.getName());
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = this.context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcp2DataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnNullForNonProxy() {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.decorator.exclude-beans:dataSource");
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		DataSource dataSource = this.context.getBean(DataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = this.context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isNull();
	}
}
