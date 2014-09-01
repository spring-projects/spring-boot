/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DataSourceMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class DataSourceMetricsAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noDataSource() {
		load();
		assertEquals(0, this.context.getBeansOfType(PublicMetrics.class).size());
	}

	@Test
	public void autoDataSource() {
		load(DataSourceAutoConfiguration.class);
		PublicMetrics bean = this.context.getBean(PublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.primary.active", "datasource.primary.usage");
	}

	@Test
	public void multipleDataSources() {
		load(MultipleDataSourcesConfig.class);
		PublicMetrics bean = this.context.getBean(PublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.tomcat.active", "datasource.tomcat.usage",
				"datasource.commonsDbcp.active", "datasource.commonsDbcp.usage");

		// Hikari won't work unless a first connection has been retrieved
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.context.getBean("hikariDS",
				DataSource.class));
		jdbcTemplate.execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection connection) throws SQLException,
					DataAccessException {
				return null;
			}
		});

		Collection<Metric<?>> anotherMetrics = bean.metrics();
		assertMetrics(anotherMetrics, "datasource.tomcat.active",
				"datasource.tomcat.usage", "datasource.hikariDS.active",
				"datasource.hikariDS.usage", "datasource.commonsDbcp.active",
				"datasource.commonsDbcp.usage");
	}

	@Test
	public void multipleDataSourcesWithPrimary() {
		load(MultipleDataSourcesWithPrimaryConfig.class);
		PublicMetrics bean = this.context.getBean(PublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.primary.active", "datasource.primary.usage",
				"datasource.commonsDbcp.active", "datasource.commonsDbcp.usage");
	}

	@Test
	public void customPrefix() {
		load(MultipleDataSourcesWithPrimaryConfig.class,
				CustomDataSourcePublicMetrics.class);
		PublicMetrics bean = this.context.getBean(PublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "ds.first.active", "ds.first.usage", "ds.second.active",
				"ds.second.usage");

	}

	private void assertMetrics(Collection<Metric<?>> metrics, String... keys) {
		Map<String, Number> content = new HashMap<String, Number>();
		for (Metric<?> metric : metrics) {
			content.put(metric.getName(), metric.getValue());
		}
		for (String key : keys) {
			assertTrue("Key '" + key + "' was not found", content.containsKey(key));
		}
	}

	private void load(Class<?>... config) {
		this.context = new AnnotationConfigApplicationContext();
		if (config.length > 0) {
			this.context.register(config);
		}
		this.context.register(DataSourcePoolMetadataProvidersConfiguration.class,
				DataSourceMetricsAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	static class MultipleDataSourcesConfig {

		@Bean
		public DataSource tomcatDataSource() {
			return initializeBuilder().type(org.apache.tomcat.jdbc.pool.DataSource.class)
					.build();
		}

		@Bean
		public DataSource hikariDS() {
			return initializeBuilder().type(HikariDataSource.class).build();
		}

		@Bean
		public DataSource commonsDbcpDataSource() {
			return initializeBuilder().type(BasicDataSource.class).build();
		}
	}

	@Configuration
	static class MultipleDataSourcesWithPrimaryConfig {

		@Bean
		@Primary
		public DataSource myDataSource() {
			return initializeBuilder().type(org.apache.tomcat.jdbc.pool.DataSource.class)
					.build();
		}

		@Bean
		public DataSource commonsDbcpDataSource() {
			return initializeBuilder().type(BasicDataSource.class).build();
		}
	}

	@Configuration
	static class CustomDataSourcePublicMetrics {

		@Bean
		public DataSourcePublicMetrics myDataSourcePublicMetrics() {
			return new DataSourcePublicMetrics() {
				@Override
				protected String createPrefix(String dataSourceName,
						DataSource dataSource, boolean primary) {
					return (primary ? "ds.first." : "ds.second");
				}
			};
		}
	}

	private static DataSourceBuilder initializeBuilder() {
		return DataSourceBuilder.create().driverClassName("org.hsqldb.jdbc.JDBCDriver")
				.url("jdbc:hsqldb:mem:test").username("sa");
	}

}
