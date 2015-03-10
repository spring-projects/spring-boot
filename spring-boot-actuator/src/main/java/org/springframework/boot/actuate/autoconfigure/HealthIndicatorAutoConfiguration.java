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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.solr.client.solrj.SolrServer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthIndicator}s.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@Configuration
@AutoConfigureBefore({ EndpointAutoConfiguration.class })
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
		MongoDataAutoConfiguration.class, RedisAutoConfiguration.class,
		RabbitAutoConfiguration.class, SolrAutoConfiguration.class,
		MailSenderAutoConfiguration.class, JmsAutoConfiguration.class})
@EnableConfigurationProperties({ HealthIndicatorAutoConfigurationProperties.class })
public class HealthIndicatorAutoConfiguration {

	@Autowired
	private HealthIndicatorAutoConfigurationProperties configurationProperties = new HealthIndicatorAutoConfigurationProperties();

	@Bean
	@ConditionalOnMissingBean
	public HealthAggregator healthAggregator() {
		OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();
		if (this.configurationProperties.getOrder() != null) {
			healthAggregator.setStatusOrder(this.configurationProperties.getOrder());
		}
		return healthAggregator;
	}

	@Bean
	@ConditionalOnMissingBean(HealthIndicator.class)
	public HealthIndicator applicationHealthIndicator() {
		return new ApplicationHealthIndicator();
	}

	@Configuration
	@ConditionalOnBean(DataSource.class)
	@ConditionalOnProperty(prefix = "management.health.db", name = "enabled", matchIfMissing = true)
	public static class DataSourcesHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired(required = false)
		private Map<String, DataSource> dataSources;

		@Autowired(required = false)
		private Collection<DataSourcePoolMetadataProvider> metadataProviders = Collections
				.emptyList();

		@Bean
		@ConditionalOnMissingBean(name = "dbHealthIndicator")
		public HealthIndicator dbHealthIndicator() {
			DataSourcePoolMetadataProvider metadataProvider = new DataSourcePoolMetadataProviders(
					this.metadataProviders);
			if (this.dataSources.size() == 1) {
				DataSource dataSource = this.dataSources.values().iterator().next();
				return createDataSourceHealthIndicator(metadataProvider, dataSource);
			}
			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, DataSource> entry : this.dataSources.entrySet()) {
				String name = entry.getKey();
				DataSource dataSource = entry.getValue();
				composite.addHealthIndicator(name,
						createDataSourceHealthIndicator(metadataProvider, dataSource));
			}
			return composite;
		}

		private DataSourceHealthIndicator createDataSourceHealthIndicator(
				DataSourcePoolMetadataProvider provider, DataSource dataSource) {
			String validationQuery = null;
			DataSourcePoolMetadata poolMetadata = provider
					.getDataSourcePoolMetadata(dataSource);
			if (poolMetadata != null) {
				validationQuery = poolMetadata.getValidationQuery();
			}
			return new DataSourceHealthIndicator(dataSource, validationQuery);
		}
	}

	@Configuration
	@ConditionalOnBean(MongoTemplate.class)
	@ConditionalOnProperty(prefix = "management.health.mongo", name = "enabled", matchIfMissing = true)
	public static class MongoHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired
		private Map<String, MongoTemplate> mongoTemplates;

		@Bean
		@ConditionalOnMissingBean(name = "mongoHealthIndicator")
		public HealthIndicator mongoHealthIndicator() {
			if (this.mongoTemplates.size() == 1) {
				return new MongoHealthIndicator(this.mongoTemplates.values().iterator()
						.next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, MongoTemplate> entry : this.mongoTemplates.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new MongoHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnBean(RedisConnectionFactory.class)
	@ConditionalOnProperty(prefix = "management.health.redis", name = "enabled", matchIfMissing = true)
	public static class RedisHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired
		private Map<String, RedisConnectionFactory> redisConnectionFactories;

		@Bean
		@ConditionalOnMissingBean(name = "redisHealthIndicator")
		public HealthIndicator redisHealthIndicator() {
			if (this.redisConnectionFactories.size() == 1) {
				return new RedisHealthIndicator(this.redisConnectionFactories.values()
						.iterator().next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, RedisConnectionFactory> entry : this.redisConnectionFactories
					.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new RedisHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnBean(RabbitTemplate.class)
	@ConditionalOnProperty(prefix = "management.health.rabbit", name = "enabled", matchIfMissing = true)
	public static class RabbitHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired
		private Map<String, RabbitTemplate> rabbitTemplates;

		@Bean
		@ConditionalOnMissingBean(name = "rabbitHealthIndicator")
		public HealthIndicator rabbitHealthIndicator() {
			if (this.rabbitTemplates.size() == 1) {
				return new RabbitHealthIndicator(this.rabbitTemplates.values().iterator()
						.next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, RabbitTemplate> entry : this.rabbitTemplates
					.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new RabbitHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnBean(SolrServer.class)
	@ConditionalOnProperty(prefix = "management.health.solr", name = "enabled", matchIfMissing = true)
	public static class SolrHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired
		private Map<String, SolrServer> solrServers;

		@Bean
		@ConditionalOnMissingBean(name = "solrHealthIndicator")
		public HealthIndicator solrHealthIndicator() {
			if (this.solrServers.size() == 1) {
				return new SolrHealthIndicator(this.solrServers.entrySet().iterator()
						.next().getValue());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, SolrServer> entry : this.solrServers.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new SolrHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnProperty(prefix = "management.health.diskspace", name = "enabled", matchIfMissing = true)
	public static class DiskSpaceHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "diskSpaceHealthIndicator")
		public HealthIndicator diskSpaceHealthIndicator(
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
	@ConditionalOnProperty(prefix = "management.health.mail", name = "enabled", matchIfMissing = true)
	public static class MailHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired(required = false)
		private Map<String, JavaMailSenderImpl> mailSenders;

		@Bean
		@ConditionalOnMissingBean(name = "mailHealthIndicator")
		public HealthIndicator mailHealthIndicator() {
			if (this.mailSenders.size() == 1) {
				JavaMailSenderImpl mailSender = this.mailSenders.values().iterator()
						.next();
				return createMailHealthIndicator(mailSender);
			}
			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, JavaMailSenderImpl> entry : this.mailSenders
					.entrySet()) {
				String name = entry.getKey();
				JavaMailSenderImpl mailSender = entry.getValue();
				composite.addHealthIndicator(name, createMailHealthIndicator(mailSender));
			}
			return composite;
		}

		private MailHealthIndicator createMailHealthIndicator(
				JavaMailSenderImpl mailSender) {
			return new MailHealthIndicator(mailSender);
		}
	}

	@Configuration
	@ConditionalOnBean(ConnectionFactory.class)
	@ConditionalOnProperty(prefix = "management.health.jms", name = "enabled", matchIfMissing = true)
	public static class JmsHealthIndicatorConfiguration {

		@Autowired
		private HealthAggregator healthAggregator;

		@Autowired(required = false)
		private Map<String, ConnectionFactory> connectionFactories;

		@Bean
		@ConditionalOnMissingBean(name = "jmsHealthIndicator")
		public HealthIndicator jmsHealthIndicator() {
			if (this.connectionFactories.size() == 1) {
				ConnectionFactory connectionFactory = this.connectionFactories.values()
						.iterator().next();
				return createJmsHealthIndicator(connectionFactory);
			}
			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, ConnectionFactory> entry : this.connectionFactories
					.entrySet()) {
				String name = entry.getKey();
				ConnectionFactory connectionFactory = entry.getValue();
				composite.addHealthIndicator(name, createJmsHealthIndicator(connectionFactory));
			}
			return composite;
		}

		private JmsHealthIndicator createJmsHealthIndicator(
				ConnectionFactory connectionFactory) {
			return new JmsHealthIndicator(connectionFactory);
		}
	}

}
