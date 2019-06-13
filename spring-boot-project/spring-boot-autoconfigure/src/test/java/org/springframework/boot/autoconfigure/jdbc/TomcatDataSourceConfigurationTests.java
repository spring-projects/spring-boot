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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link TomcatDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class TomcatDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.tomcat.";

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@BeforeEach
	public void init() {
		TestPropertyValues.of(PREFIX + "initialize:false").applyTo(this.context);
	}

	@Test
	void testDataSourceExists() {
		this.context.register(TomcatDataSourceConfiguration.class);
		TestPropertyValues.of(PREFIX + "url:jdbc:h2:mem:testdb").applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class)).isNotNull();
	}

	@Test
	void testDataSourcePropertiesOverridden() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		TestPropertyValues
				.of(PREFIX + "url:jdbc:h2:mem:testdb", PREFIX + "testWhileIdle:true", PREFIX + "testOnBorrow:true",
						PREFIX + "testOnReturn:true", PREFIX + "timeBetweenEvictionRunsMillis:10000",
						PREFIX + "minEvictableIdleTimeMillis:12345", PREFIX + "maxWait:1234",
						PREFIX + "jdbcInterceptors:SlowQueryReport", PREFIX + "validationInterval:9999")
				.applyTo(this.context);
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertThat(ds.getUrl()).isEqualTo("jdbc:h2:mem:testdb");
		assertThat(ds.isTestWhileIdle()).isTrue();
		assertThat(ds.isTestOnBorrow()).isTrue();
		assertThat(ds.isTestOnReturn()).isTrue();
		assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(10000);
		assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(12345);
		assertThat(ds.getMaxWait()).isEqualTo(1234);
		assertThat(ds.getValidationInterval()).isEqualTo(9999L);
		assertDataSourceHasInterceptors(ds);
	}

	private void assertDataSourceHasInterceptors(DataSourceProxy ds) throws ClassNotFoundException {
		PoolProperties.InterceptorDefinition[] interceptors = ds.getJdbcInterceptorsAsArray();
		for (PoolProperties.InterceptorDefinition interceptor : interceptors) {
			if (SlowQueryReport.class == interceptor.getInterceptorClass()) {
				return;
			}
		}
		fail("SlowQueryReport interceptor should have been set.");
	}

	@Test
	void testDataSourceDefaultsPreserved() {
		this.context.register(TomcatDataSourceConfiguration.class);
		TestPropertyValues.of(PREFIX + "url:jdbc:h2:mem:testdb").applyTo(this.context);
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(5000);
		assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(60000);
		assertThat(ds.getMaxWait()).isEqualTo(30000);
		assertThat(ds.getValidationInterval()).isEqualTo(3000L);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@EnableMBeanExport
	protected static class TomcatDataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.tomcat")
		public DataSource dataSource() {
			return DataSourceBuilder.create().type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

	}

}
