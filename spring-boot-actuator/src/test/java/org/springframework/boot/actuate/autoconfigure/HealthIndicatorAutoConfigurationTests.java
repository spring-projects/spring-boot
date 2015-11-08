/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.CassandraHealthIndicator;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicator;
import org.springframework.boot.actuate.health.ElasticsearchHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.JmsHealthIndicator;
import org.springframework.boot.actuate.health.MailHealthIndicator;
import org.springframework.boot.actuate.health.MongoHealthIndicator;
import org.springframework.boot.actuate.health.RabbitHealthIndicator;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.actuate.health.SolrHealthIndicator;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthIndicatorAutoConfiguration}.
 *
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class HealthIndicatorAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultHealthIndicator() {
		this.context.register(HealthIndicatorAutoConfiguration.class,
				ManagementServerProperties.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void defaultHealthIndicatorsDisabled() {
		this.context.register(HealthIndicatorAutoConfiguration.class,
				ManagementServerProperties.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.defaults.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void defaultHealthIndicatorsDisabledWithCustomOne() {
		this.context.register(CustomHealthIndicator.class,
				HealthIndicatorAutoConfiguration.class, ManagementServerProperties.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.defaults.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertSame(this.context.getBean("customHealthIndicator"),
				beans.values().iterator().next());
	}

	@Test
	public void defaultHealthIndicatorsDisabledButOne() {
		this.context.register(HealthIndicatorAutoConfiguration.class,
				ManagementServerProperties.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.defaults.enabled:false",
				"management.health.diskspace.enabled:true");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(DiskSpaceHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void redisHealthIndicator() {
		this.context.register(RedisAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(RedisHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notRedisHealthIndicator() {
		this.context.register(RedisAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.redis.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void mongoHealthIndicator() {
		this.context.register(MongoAutoConfiguration.class,
				ManagementServerProperties.class, MongoDataAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(MongoHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notMongoHealthIndicator() {
		this.context.register(MongoAutoConfiguration.class,
				ManagementServerProperties.class, MongoDataAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.mongo.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void combinedHealthIndicator() {
		this.context.register(MongoAutoConfiguration.class, RedisAutoConfiguration.class,
				MongoDataAutoConfiguration.class, SolrAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(4, beans.size());
	}

	@Test
	public void dataSourceHealthIndicator() {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(DataSourceHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void dataSourceHealthIndicatorWithCustomValidationQuery() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				ManagementServerProperties.class, DataSourceProperties.class,
				DataSourceConfig.class,
				DataSourcePoolMetadataProvidersConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.validation-query:SELECT from FOOBAR",
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		HealthIndicator healthIndicator = beans.values().iterator().next();
		assertEquals(DataSourceHealthIndicator.class, healthIndicator.getClass());
		DataSourceHealthIndicator dataSourceHealthIndicator = (DataSourceHealthIndicator) healthIndicator;
		assertEquals("SELECT from FOOBAR", dataSourceHealthIndicator.getQuery());
	}

	@Test
	public void notDataSourceHealthIndicator() {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.db.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void rabbitHealthIndicator() {
		this.context.register(RabbitAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(RabbitHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notRabbitHealthIndicator() {
		this.context.register(RabbitAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.rabbit.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void solrHeathIndicator() {
		this.context.register(SolrAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(SolrHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notSolrHeathIndicator() {
		this.context.register(SolrAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.solr.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void diskSpaceHealthIndicator() {
		this.context.register(HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(DiskSpaceHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void mailHealthIndicator() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mail.host:smtp.acme.org",
				"management.health.diskspace.enabled:false");
		this.context.register(MailSenderAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();

		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(MailHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notMailHealthIndicator() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mail.host:smtp.acme.org", "management.health.mail.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.register(MailSenderAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();

		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void jmsHealthIndicator() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.register(ActiveMQAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();

		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(JmsHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notJmsHealthIndicator() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.jms.enabled:false",
				"management.health.diskspace.enabled:false");
		this.context.register(ActiveMQAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();

		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void elasticSearchHealthIndicator() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.elasticsearch.properties.path.data:target/data",
				"spring.data.elasticsearch.properties.path.logs:target/logs",
				"management.health.diskspace.enabled:false");
		this.context.register(ElasticsearchAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();

		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ElasticsearchHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void notElasticSearchHealthIndicator() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.elasticsearch.enabled:false",
				"spring.data.elasticsearch.properties.path.data:target/data",
				"spring.data.elasticsearch.properties.path.logs:target/logs",
				"management.health.diskspace.enabled:false");
		this.context.register(ElasticsearchAutoConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();

		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(ApplicationHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Test
	public void cassandraHealthIndicator() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.health.diskspace.enabled:false");
		this.context.register(CassandraConfiguration.class,
				ManagementServerProperties.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(CassandraHealthIndicator.class,
				beans.values().iterator().next().getClass());
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class DataSourceConfig {

		@Bean
		@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
		public DataSource dataSource() {
			return DataSourceBuilder.create()
					.driverClassName("org.hsqldb.jdbc.JDBCDriver")
					.url("jdbc:hsqldb:mem:test").username("sa").build();
		}

	}

	@Configuration
	protected static class CustomHealthIndicator {

		@Bean
		public HealthIndicator customHealthIndicator() {
			return new HealthIndicator() {
				@Override
				public Health health() {
					return Health.down().build();
				}
			};
		}

	}

	@Configuration
	protected static class CassandraConfiguration {

		@Bean
		public CassandraOperations cassandraOperations() {
			CassandraOperations operations = mock(CassandraOperations.class);
			return operations;
		}

	}

}
