/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Collection;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import com.couchbase.client.java.Bucket;
import com.datastax.driver.core.Cluster;
import org.apache.solr.client.solrj.SolrClient;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.CassandraHealthIndicator;
import org.springframework.boot.actuate.health.CouchbaseHealthIndicator;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.JmsHealthIndicator;
import org.springframework.boot.actuate.health.MailHealthIndicator;
import org.springframework.boot.actuate.health.MongoHealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.RabbitHealthIndicator;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.actuate.health.SolrHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthIndicator}s.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @since 1.1.0
 */
@Configuration
@AutoConfigureBefore({ EndpointAutoConfiguration.class })
@AutoConfigureAfter({ CassandraAutoConfiguration.class,
		CassandraDataAutoConfiguration.class, CouchbaseAutoConfiguration.class,
		DataSourceAutoConfiguration.class, ElasticsearchAutoConfiguration.class,
		JestAutoConfiguration.class, JmsAutoConfiguration.class,
		MailSenderAutoConfiguration.class, MongoAutoConfiguration.class,
		MongoDataAutoConfiguration.class, RabbitAutoConfiguration.class,
		RedisAutoConfiguration.class, SolrAutoConfiguration.class })
@EnableConfigurationProperties({ HealthIndicatorProperties.class })
@Import({ ElasticsearchHealthIndicatorConfiguration.ElasticsearchClientHealthIndicatorConfiguration.class,
		ElasticsearchHealthIndicatorConfiguration.ElasticsearchJestHealthIndicatorConfiguration.class })
public class HealthIndicatorAutoConfiguration {

	private final HealthIndicatorProperties properties;

	public HealthIndicatorAutoConfiguration(HealthIndicatorProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(HealthAggregator.class)
	public OrderedHealthAggregator healthAggregator() {
		OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();
		if (this.properties.getOrder() != null) {
			healthAggregator.setStatusOrder(this.properties.getOrder());
		}
		return healthAggregator;
	}

	@Bean
	@ConditionalOnMissingBean(HealthIndicator.class)
	public ApplicationHealthIndicator applicationHealthIndicator() {
		return new ApplicationHealthIndicator();
	}

	@Configuration
	@ConditionalOnClass({ CassandraOperations.class, Cluster.class })
	@ConditionalOnBean(CassandraOperations.class)
	@ConditionalOnEnabledHealthIndicator("cassandra")
	public static class CassandraHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<CassandraHealthIndicator, CassandraOperations> {

		private final Map<String, CassandraOperations> cassandraOperations;

		public CassandraHealthIndicatorConfiguration(
				Map<String, CassandraOperations> cassandraOperations) {
			this.cassandraOperations = cassandraOperations;
		}

		@Bean
		@ConditionalOnMissingBean(name = "cassandraHealthIndicator")
		public HealthIndicator cassandraHealthIndicator() {
			return createHealthIndicator(this.cassandraOperations);
		}

	}

	@Configuration
	@ConditionalOnClass({ CouchbaseOperations.class, Bucket.class })
	@ConditionalOnBean(CouchbaseOperations.class)
	@ConditionalOnEnabledHealthIndicator("couchbase")
	public static class CouchbaseHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<CouchbaseHealthIndicator, CouchbaseOperations> {

		private final Map<String, CouchbaseOperations> couchbaseOperations;

		public CouchbaseHealthIndicatorConfiguration(
				Map<String, CouchbaseOperations> couchbaseOperations) {
			this.couchbaseOperations = couchbaseOperations;
		}

		@Bean
		@ConditionalOnMissingBean(name = "couchbaseHealthIndicator")
		public HealthIndicator couchbaseHealthIndicator() {
			return createHealthIndicator(this.couchbaseOperations);
		}

	}

	@Configuration
	@ConditionalOnClass(JdbcTemplate.class)
	@ConditionalOnBean(DataSource.class)
	@ConditionalOnEnabledHealthIndicator("db")
	public static class DataSourcesHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<DataSourceHealthIndicator, DataSource>
			implements InitializingBean {

		private final Map<String, DataSource> dataSources;

		private final Collection<DataSourcePoolMetadataProvider> metadataProviders;

		private DataSourcePoolMetadataProvider poolMetadataProvider;

		public DataSourcesHealthIndicatorConfiguration(
				ObjectProvider<Map<String, DataSource>> dataSourcesProvider,
				ObjectProvider<Collection<DataSourcePoolMetadataProvider>> metadataProvidersProvider) {
			this.dataSources = dataSourcesProvider.getIfAvailable();
			this.metadataProviders = metadataProvidersProvider.getIfAvailable();
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			this.poolMetadataProvider = new DataSourcePoolMetadataProviders(
					this.metadataProviders);
		}

		@Bean
		@ConditionalOnMissingBean(name = "dbHealthIndicator")
		public HealthIndicator dbHealthIndicator() {
			return createHealthIndicator(this.dataSources);
		}

		@Override
		protected DataSourceHealthIndicator createHealthIndicator(DataSource source) {
			return new DataSourceHealthIndicator(source, getValidationQuery(source));
		}

		private String getValidationQuery(DataSource source) {
			DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider
					.getDataSourcePoolMetadata(source);
			return (poolMetadata == null ? null : poolMetadata.getValidationQuery());
		}

	}

	@Configuration
	@ConditionalOnBean(MongoTemplate.class)
	@ConditionalOnEnabledHealthIndicator("mongo")
	public static class MongoHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<MongoHealthIndicator, MongoTemplate> {

		private final Map<String, MongoTemplate> mongoTemplates;

		public MongoHealthIndicatorConfiguration(
				Map<String, MongoTemplate> mongoTemplates) {
			this.mongoTemplates = mongoTemplates;
		}

		@Bean
		@ConditionalOnMissingBean(name = "mongoHealthIndicator")
		public HealthIndicator mongoHealthIndicator() {
			return createHealthIndicator(this.mongoTemplates);
		}

	}

	@Configuration
	@ConditionalOnBean(RedisConnectionFactory.class)
	@ConditionalOnEnabledHealthIndicator("redis")
	public static class RedisHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<RedisHealthIndicator, RedisConnectionFactory> {

		private final Map<String, RedisConnectionFactory> redisConnectionFactories;

		public RedisHealthIndicatorConfiguration(
				Map<String, RedisConnectionFactory> redisConnectionFactories) {
			this.redisConnectionFactories = redisConnectionFactories;
		}

		@Bean
		@ConditionalOnMissingBean(name = "redisHealthIndicator")
		public HealthIndicator redisHealthIndicator() {
			return createHealthIndicator(this.redisConnectionFactories);
		}

	}

	@Configuration
	@ConditionalOnBean(RabbitTemplate.class)
	@ConditionalOnEnabledHealthIndicator("rabbit")
	public static class RabbitHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<RabbitHealthIndicator, RabbitTemplate> {

		private final Map<String, RabbitTemplate> rabbitTemplates;

		public RabbitHealthIndicatorConfiguration(
				Map<String, RabbitTemplate> rabbitTemplates) {
			this.rabbitTemplates = rabbitTemplates;
		}

		@Bean
		@ConditionalOnMissingBean(name = "rabbitHealthIndicator")
		public HealthIndicator rabbitHealthIndicator() {
			return createHealthIndicator(this.rabbitTemplates);
		}

	}

	@Configuration
	@ConditionalOnBean(SolrClient.class)
	@ConditionalOnEnabledHealthIndicator("solr")
	public static class SolrHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<SolrHealthIndicator, SolrClient> {

		private final Map<String, SolrClient> solrClients;

		public SolrHealthIndicatorConfiguration(Map<String, SolrClient> solrClients) {
			this.solrClients = solrClients;
		}

		@Bean
		@ConditionalOnMissingBean(name = "solrHealthIndicator")
		public HealthIndicator solrHealthIndicator() {
			return createHealthIndicator(this.solrClients);
		}

	}

	@Configuration
	@ConditionalOnEnabledHealthIndicator("diskspace")
	public static class DiskSpaceHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "diskSpaceHealthIndicator")
		public DiskSpaceHealthIndicator diskSpaceHealthIndicator(
				DiskSpaceHealthIndicatorProperties properties) {
			return new DiskSpaceHealthIndicator(properties);
		}

		@Bean
		public DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties() {
			return new DiskSpaceHealthIndicatorProperties();
		}

	}

	@Configuration
	@ConditionalOnBean(JavaMailSenderImpl.class)
	@ConditionalOnEnabledHealthIndicator("mail")
	public static class MailHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<MailHealthIndicator, JavaMailSenderImpl> {

		private final Map<String, JavaMailSenderImpl> mailSenders;

		public MailHealthIndicatorConfiguration(
				ObjectProvider<Map<String, JavaMailSenderImpl>> mailSendersProvider) {
			this.mailSenders = mailSendersProvider.getIfAvailable();
		}

		@Bean
		@ConditionalOnMissingBean(name = "mailHealthIndicator")
		public HealthIndicator mailHealthIndicator() {
			return createHealthIndicator(this.mailSenders);
		}

	}

	@Configuration
	@ConditionalOnBean(ConnectionFactory.class)
	@ConditionalOnEnabledHealthIndicator("jms")
	public static class JmsHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<JmsHealthIndicator, ConnectionFactory> {

		private final Map<String, ConnectionFactory> connectionFactories;

		public JmsHealthIndicatorConfiguration(
				ObjectProvider<Map<String, ConnectionFactory>> connectionFactoriesProvider) {
			this.connectionFactories = connectionFactoriesProvider.getIfAvailable();
		}

		@Bean
		@ConditionalOnMissingBean(name = "jmsHealthIndicator")
		public HealthIndicator jmsHealthIndicator() {
			return createHealthIndicator(this.connectionFactories);
		}

	}

}
