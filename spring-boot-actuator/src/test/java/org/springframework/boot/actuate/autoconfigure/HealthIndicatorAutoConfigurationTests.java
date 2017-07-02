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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Map;

import javax.sql.DataSource;

import io.searchbox.client.JestClient;
import org.junit.Test;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.CassandraHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.CouchbaseHealthIndicator;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicator;
import org.springframework.boot.actuate.health.ElasticsearchHealthIndicator;
import org.springframework.boot.actuate.health.ElasticsearchJestHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.JmsHealthIndicator;
import org.springframework.boot.actuate.health.LdapHealthIndicator;
import org.springframework.boot.actuate.health.MailHealthIndicator;
import org.springframework.boot.actuate.health.MongoHealthIndicator;
import org.springframework.boot.actuate.health.Neo4jHealthIndicator;
import org.springframework.boot.actuate.health.RabbitHealthIndicator;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.actuate.health.SolrHealthIndicator;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ContextConsumer;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.ldap.core.LdapOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthIndicatorAutoConfiguration}.
 *
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Eric Spiegelberg
 */
public class HealthIndicatorAutoConfigurationTests {

	public final ContextLoader<AnnotationConfigApplicationContext> contextLoader = ContextLoader
			.standard().autoConfig(HealthIndicatorAutoConfiguration.class,
					ManagementServerProperties.class);

	@Test
	public void defaultHealthIndicator() {
		this.contextLoader.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void defaultHealthIndicatorsDisabled() {
		this.contextLoader.env("management.health.defaults.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void defaultHealthIndicatorsDisabledWithCustomOne() {
		this.contextLoader.config(CustomHealthIndicator.class)
				.env("management.health.defaults.enabled:false").load(context -> {
					Map<String, HealthIndicator> beans = context
							.getBeansOfType(HealthIndicator.class);
					assertThat(beans).hasSize(1);
					assertThat(context.getBean("customHealthIndicator"))
							.isSameAs(beans.values().iterator().next());
				});
	}

	@Test
	public void defaultHealthIndicatorsDisabledButOne() {
		this.contextLoader
				.env("management.health.defaults.enabled:false",
						"management.health.diskspace.enabled:true")
				.load(hasSingleHealthIndicator(DiskSpaceHealthIndicator.class));
	}

	@Test
	public void redisHealthIndicator() {
		this.contextLoader.autoConfigFirst(RedisAutoConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(RedisHealthIndicator.class));
	}

	@Test
	public void notRedisHealthIndicator() {
		this.contextLoader.autoConfigFirst(RedisAutoConfiguration.class)
				.env("management.health.redis.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void mongoHealthIndicator() {
		this.contextLoader
				.autoConfigFirst(MongoAutoConfiguration.class,
						MongoDataAutoConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(MongoHealthIndicator.class));
	}

	@Test
	public void notMongoHealthIndicator() {
		this.contextLoader
				.autoConfigFirst(MongoAutoConfiguration.class,
						MongoDataAutoConfiguration.class)
				.env("management.health.mongo.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void combinedHealthIndicator() {
		this.contextLoader.autoConfigFirst(MongoAutoConfiguration.class,
				RedisAutoConfiguration.class, MongoDataAutoConfiguration.class,
				SolrAutoConfiguration.class).load(context -> {
					Map<String, HealthIndicator> beans = context
							.getBeansOfType(HealthIndicator.class);
					assertThat(beans).hasSize(4);
				});
	}

	@Test
	public void dataSourceHealthIndicator() {
		this.contextLoader.autoConfigFirst(EmbeddedDataSourceConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(DataSourceHealthIndicator.class));
	}

	@Test
	public void dataSourceHealthIndicatorWithSeveralDataSources() {
		this.contextLoader
				.config(EmbeddedDataSourceConfiguration.class, DataSourceConfig.class)
				.env("management.health.diskspace.enabled:false").load(context -> {
					Map<String, HealthIndicator> beans = context
							.getBeansOfType(HealthIndicator.class);
					assertThat(beans).hasSize(1);
					HealthIndicator bean = beans.values().iterator().next();
					assertThat(bean).isExactlyInstanceOf(CompositeHealthIndicator.class);
					assertThat(bean.health().getDetails()).containsOnlyKeys("dataSource",
							"testDataSource");
				});
	}

	@Test
	public void dataSourceHealthIndicatorWithAbstractRoutingDataSource() {
		this.contextLoader
				.config(EmbeddedDataSourceConfiguration.class,
						RoutingDatasourceConfig.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(DataSourceHealthIndicator.class));
	}

	@Test
	public void dataSourceHealthIndicatorWithCustomValidationQuery() {
		this.contextLoader
				.config(DataSourceConfig.class,
						DataSourcePoolMetadataProvidersConfiguration.class,
						HealthIndicatorAutoConfiguration.class)
				.env("spring.datasource.test.validation-query:SELECT from FOOBAR",
						"management.health.diskspace.enabled:false")
				.load(context -> {
					Map<String, HealthIndicator> beans = context
							.getBeansOfType(HealthIndicator.class);
					assertThat(beans).hasSize(1);
					HealthIndicator healthIndicator = beans.values().iterator().next();
					assertThat(healthIndicator.getClass())
							.isEqualTo(DataSourceHealthIndicator.class);
					DataSourceHealthIndicator dataSourceHealthIndicator = (DataSourceHealthIndicator) healthIndicator;
					assertThat(dataSourceHealthIndicator.getQuery())
							.isEqualTo("SELECT from FOOBAR");
				});
	}

	@Test
	public void notDataSourceHealthIndicator() {
		this.contextLoader.config(EmbeddedDataSourceConfiguration.class)
				.env("management.health.db.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void rabbitHealthIndicator() {
		this.contextLoader.autoConfigFirst(RabbitAutoConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(RabbitHealthIndicator.class));
	}

	@Test
	public void notRabbitHealthIndicator() {
		this.contextLoader.autoConfigFirst(RabbitAutoConfiguration.class)
				.env("management.health.rabbit.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void solrHealthIndicator() {
		this.contextLoader.autoConfigFirst(SolrAutoConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(SolrHealthIndicator.class));
	}

	@Test
	public void notSolrHealthIndicator() {
		this.contextLoader.autoConfigFirst(SolrAutoConfiguration.class)
				.env("management.health.solr.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void diskSpaceHealthIndicator() {
		this.contextLoader.load(hasSingleHealthIndicator(DiskSpaceHealthIndicator.class));
	}

	@Test
	public void mailHealthIndicator() {
		this.contextLoader.autoConfigFirst(MailSenderAutoConfiguration.class)
				.env("spring.mail.host:smtp.acme.org",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(MailHealthIndicator.class));
	}

	@Test
	public void notMailHealthIndicator() {
		this.contextLoader.autoConfigFirst(MailSenderAutoConfiguration.class)
				.env("spring.mail.host:smtp.acme.org",
						"management.health.mail.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void jmsHealthIndicator() {
		this.contextLoader.autoConfigFirst(ActiveMQAutoConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(JmsHealthIndicator.class));
	}

	@Test
	public void notJmsHealthIndicator() {
		this.contextLoader.autoConfigFirst(ActiveMQAutoConfiguration.class)
				.env("management.health.jms.enabled:false",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void elasticsearchHealthIndicator() {
		this.contextLoader
				.autoConfigFirst(JestClientConfiguration.class,
						JestAutoConfiguration.class, ElasticsearchAutoConfiguration.class)
				.env("spring.data.elasticsearch.cluster-nodes:localhost:0",
						"management.health.diskspace.enabled:false")
				.systemProperty("es.set.netty.runtime.available.processors", "false")
				.load(hasSingleHealthIndicator(ElasticsearchHealthIndicator.class));
	}

	@Test
	public void elasticsearchJestHealthIndicator() {
		this.contextLoader
				.autoConfigFirst(JestClientConfiguration.class,
						JestAutoConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.systemProperty("es.set.netty.runtime.available.processors", "false")
				.load(hasSingleHealthIndicator(ElasticsearchJestHealthIndicator.class));
	}

	@Test
	public void notElasticsearchHealthIndicator() {
		this.contextLoader
				.autoConfigFirst(JestClientConfiguration.class,
						JestAutoConfiguration.class, ElasticsearchAutoConfiguration.class)
				.env("management.health.elasticsearch.enabled:false",
						"spring.data.elasticsearch.properties.path.home:target",
						"management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void cassandraHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(CassandraConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(CassandraHealthIndicator.class));
	}

	@Test
	public void notCassandraHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(CassandraConfiguration.class)
				.env("management.health.diskspace.enabled:false",
						"management.health.cassandra.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void couchbaseHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(CouchbaseConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(CouchbaseHealthIndicator.class));
	}

	@Test
	public void notCouchbaseHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(CouchbaseConfiguration.class)
				.env("management.health.diskspace.enabled:false",
						"management.health.couchbase.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void ldapHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(LdapConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(LdapHealthIndicator.class));
	}

	@Test
	public void notLdapHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(LdapConfiguration.class)
				.env("management.health.diskspace.enabled:false",
						"management.health.ldap.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void neo4jHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(Neo4jConfiguration.class)
				.env("management.health.diskspace.enabled:false")
				.load(hasSingleHealthIndicator(Neo4jHealthIndicator.class));
	}

	@Test
	public void notNeo4jHealthIndicator() throws Exception {
		this.contextLoader.autoConfigFirst(Neo4jConfiguration.class)
				.env("management.health.diskspace.enabled:false",
						"management.health.neo4j.enabled:false")
				.load(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	private ContextConsumer<AnnotationConfigApplicationContext> hasSingleHealthIndicator(
			Class<? extends HealthIndicator> type) {
		return context -> {
			Map<String, HealthIndicator> beans = context
					.getBeansOfType(HealthIndicator.class);
			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next().getClass()).isEqualTo(type);
		};
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class DataSourceConfig {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.test")
		public DataSource testDataSource() {
			return DataSourceBuilder.create()
					.type(org.apache.tomcat.jdbc.pool.DataSource.class)
					.driverClassName("org.hsqldb.jdbc.JDBCDriver")
					.url("jdbc:hsqldb:mem:test").username("sa").build();
		}

	}

	@Configuration
	protected static class RoutingDatasourceConfig {

		@Bean
		AbstractRoutingDataSource routingDataSource() {
			return mock(AbstractRoutingDataSource.class);
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
			return mock(CassandraOperations.class);
		}

	}

	@Configuration
	protected static class CouchbaseConfiguration {

		@Bean
		public CouchbaseOperations couchbaseOperations() {
			return mock(CouchbaseOperations.class);
		}

	}

	protected static class JestClientConfiguration {

		@Bean
		public JestClient jestClient() {
			return mock(JestClient.class);
		}

	}

	@Configuration
	protected static class LdapConfiguration {

		@Bean
		public LdapOperations ldapOperations() {
			return mock(LdapOperations.class);
		}

	}

	@Configuration
	protected static class Neo4jConfiguration {

		@Bean
		public SessionFactory sessionFactory() {
			return mock(SessionFactory.class);
		}

	}

}
