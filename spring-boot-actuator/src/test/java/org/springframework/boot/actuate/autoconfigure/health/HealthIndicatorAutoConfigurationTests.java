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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import javax.sql.DataSource;

import io.searchbox.client.JestClient;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.actuate.autoconfigure.web.ManagementServerProperties;
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
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

	public final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(HealthIndicatorAutoConfiguration.class,
							ManagementServerProperties.class));

	@Test
	public void defaultHealthIndicator() {
		this.contextRunner.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void defaultHealthIndicatorsDisabled() {
		this.contextRunner.withPropertyValues("management.health.defaults.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void defaultHealthIndicatorsDisabledWithCustomOne() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicator.class)
				.withPropertyValues("management.health.defaults.enabled:false")
				.run((context) -> {
					Map<String, HealthIndicator> beans = context
							.getBeansOfType(HealthIndicator.class);
					assertThat(beans).hasSize(1);
					assertThat(context.getBean("customHealthIndicator"))
							.isSameAs(beans.values().iterator().next());
				});
	}

	@Test
	public void defaultHealthIndicatorsDisabledButOne() {
		this.contextRunner
				.withPropertyValues("management.health.defaults.enabled:false",
						"management.health.diskspace.enabled:true")
				.run(hasSingleHealthIndicator(DiskSpaceHealthIndicator.class));
	}

	@Test
	public void redisHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(RedisHealthIndicator.class));
	}

	@Test
	public void notRedisHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("management.health.redis.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void mongoHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class,
						MongoDataAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(MongoHealthIndicator.class));
	}

	@Test
	public void notMongoHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class,
						MongoDataAutoConfiguration.class))
				.withPropertyValues("management.health.mongo.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void combinedHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class,
						RedisAutoConfiguration.class, MongoDataAutoConfiguration.class,
						SolrAutoConfiguration.class))
				.run((context) -> {
					Map<String, HealthIndicator> beans = context
							.getBeansOfType(HealthIndicator.class);
					assertThat(beans).hasSize(4);
				});
	}

	@Test
	public void dataSourceHealthIndicator() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(DataSourceHealthIndicator.class));
	}

	@Test
	public void dataSourceHealthIndicatorWithSeveralDataSources() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
						DataSourceConfig.class)
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run((context) -> {
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
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
						RoutingDatasourceConfig.class)
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(DataSourceHealthIndicator.class));
	}

	@Test
	public void dataSourceHealthIndicatorWithCustomValidationQuery() {
		this.contextRunner
				.withUserConfiguration(DataSourceConfig.class,
						DataSourcePoolMetadataProvidersConfiguration.class,
						HealthIndicatorAutoConfiguration.class)
				.withPropertyValues(
						"spring.datasource.test.validation-query:SELECT from FOOBAR",
						"management.health.diskspace.enabled:false")
				.run((context) -> {
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
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("management.health.db.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void rabbitHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(RabbitHealthIndicator.class));
	}

	@Test
	public void notRabbitHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
				.withPropertyValues("management.health.rabbit.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void solrHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(SolrHealthIndicator.class));
	}

	@Test
	public void notSolrHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class))
				.withPropertyValues("management.health.solr.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void diskSpaceHealthIndicator() {
		this.contextRunner.run(hasSingleHealthIndicator(DiskSpaceHealthIndicator.class));
	}

	@Test
	public void mailHealthIndicator() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(MailSenderAutoConfiguration.class))
				.withPropertyValues("spring.mail.host:smtp.acme.org",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(MailHealthIndicator.class));
	}

	@Test
	public void notMailHealthIndicator() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(MailSenderAutoConfiguration.class))
				.withPropertyValues("spring.mail.host:smtp.acme.org",
						"management.health.mail.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void jmsHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(JmsHealthIndicator.class));
	}

	@Test
	public void notJmsHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class))
				.withPropertyValues("management.health.jms.enabled:false",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void elasticsearchHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(JestClientConfiguration.class,
						JestAutoConfiguration.class,
						ElasticsearchAutoConfiguration.class))
				.withPropertyValues("spring.data.elasticsearch.cluster-nodes:localhost:0",
						"management.health.diskspace.enabled:false")
				.withSystemProperties("es.set.netty.runtime.available.processors=false")
				.run(hasSingleHealthIndicator(ElasticsearchHealthIndicator.class));
	}

	@Test
	public void elasticsearchJestHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(JestClientConfiguration.class,
						JestAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.withSystemProperties("es.set.netty.runtime.available.processors=false")
				.run(hasSingleHealthIndicator(ElasticsearchJestHealthIndicator.class));
	}

	@Test
	public void notElasticsearchHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(JestClientConfiguration.class,
						JestAutoConfiguration.class,
						ElasticsearchAutoConfiguration.class))
				.withPropertyValues("management.health.elasticsearch.enabled:false",
						"spring.data.elasticsearch.properties.path.home:target",
						"management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void cassandraHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CassandraConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(CassandraHealthIndicator.class));
	}

	@Test
	public void notCassandraHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CassandraConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false",
						"management.health.cassandra.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void couchbaseHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CouchbaseConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(CouchbaseHealthIndicator.class));
	}

	@Test
	public void notCouchbaseHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CouchbaseConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false",
						"management.health.couchbase.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void ldapHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(LdapConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(LdapHealthIndicator.class));
	}

	@Test
	public void notLdapHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(LdapConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false",
						"management.health.ldap.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	@Test
	public void neo4jHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(Neo4jConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleHealthIndicator(Neo4jHealthIndicator.class));
	}

	@Test
	public void notNeo4jHealthIndicator() throws Exception {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(Neo4jConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false",
						"management.health.neo4j.enabled:false")
				.run(hasSingleHealthIndicator(ApplicationHealthIndicator.class));
	}

	private ContextConsumer<AssertableApplicationContext> hasSingleHealthIndicator(
			Class<? extends HealthIndicator> type) {
		return (context) -> assertThat(context).getBeans(HealthIndicator.class).hasSize(1)
				.hasValueSatisfying(
						new Condition<>((indicator) -> indicator.getClass().equals(type),
								"Wrong indicator type"));
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
			return () -> Health.down().build();
		}

	}

	@Configuration
	@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
	protected static class CassandraConfiguration {

		@Bean
		public CassandraOperations cassandraOperations() {
			return mock(CassandraOperations.class);
		}

	}

	@Configuration
	@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
	protected static class CouchbaseConfiguration {

		@Bean
		public CouchbaseOperations couchbaseOperations() {
			return mock(CouchbaseOperations.class);
		}

	}

	@Configuration
	protected static class JestClientConfiguration {

		@Bean
		public JestClient jestClient() {
			return mock(JestClient.class);
		}

	}

	@Configuration
	@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
	protected static class LdapConfiguration {

		@Bean
		public LdapOperations ldapOperations() {
			return mock(LdapOperations.class);
		}

	}

	@Configuration
	@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
	protected static class Neo4jConfiguration {

		@Bean
		public SessionFactory sessionFactory() {
			return mock(SessionFactory.class);
		}

	}

}
