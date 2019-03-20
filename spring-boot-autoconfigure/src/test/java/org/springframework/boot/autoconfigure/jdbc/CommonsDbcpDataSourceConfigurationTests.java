/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonsDbcpDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@Deprecated
public class CommonsDbcpDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.dbcp.";

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(CommonsDbcpDataSourceConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		this.context.close();
	}

	@Test
	public void testDataSourcePropertiesOverridden() throws Exception {
		this.context.register(CommonsDbcpDataSourceConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "url:jdbc:foo//bar/spam");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testWhileIdle:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testOnBorrow:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testOnReturn:true");
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "timeBetweenEvictionRunsMillis:10000");
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "minEvictableIdleTimeMillis:12345");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "maxWait:1234");
		this.context.refresh();
		BasicDataSource ds = this.context.getBean(BasicDataSource.class);
		assertThat(ds.getUrl()).isEqualTo("jdbc:foo//bar/spam");
		assertThat(ds.getTestWhileIdle()).isTrue();
		assertThat(ds.getTestOnBorrow()).isTrue();
		assertThat(ds.getTestOnReturn()).isTrue();
		assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(10000);
		assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(12345);
		assertThat(ds.getMaxWait()).isEqualTo(1234);
	}

	@Test
	public void testDataSourceDefaultsPreserved() throws Exception {
		this.context.register(CommonsDbcpDataSourceConfiguration.class);
		this.context.refresh();
		BasicDataSource ds = this.context.getBean(BasicDataSource.class);
		assertThat(ds.getTimeBetweenEvictionRunsMillis())
				.isEqualTo(GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
		assertThat(ds.getMinEvictableIdleTimeMillis())
				.isEqualTo(GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
		assertThat(ds.getMaxWait()).isEqualTo(GenericObjectPool.DEFAULT_MAX_WAIT);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class CommonsDbcpDataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp")
		public DataSource dataSource() {
			return DataSourceBuilder.create().type(BasicDataSource.class).build();
		}

	}

}
