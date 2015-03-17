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
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.solr.client.solrj.SolrServer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
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
import org.springframework.core.ResolvableType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthIndicator}s.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.1.0
 */
@Configuration
@AutoConfigureBefore({ EndpointAutoConfiguration.class })
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
		MongoDataAutoConfiguration.class, RedisAutoConfiguration.class,
		RabbitAutoConfiguration.class, SolrAutoConfiguration.class,
		MailSenderAutoConfiguration.class, JmsAutoConfiguration.class })
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

	/**
	 * Base class for configurations that can combine source beans using a
	 * {@link CompositeHealthIndicator}.
	 * @param <H> The health indicator type
	 * @param <S> The bean source type
	 */
	protected static abstract class CompositeHealthIndicatorConfiguration<H extends HealthIndicator, S> {

		@Autowired
		private HealthAggregator healthAggregator;

		protected HealthIndicator createHealthIndicator(Map<String, S> beans) {
			if (beans.size() == 1) {
				return createHealthIndicator(beans.values().iterator().next());
			}
			CompositeHealthIndicator composite = new CompositeHealthIndicator(
					this.healthAggregator);
			for (Map.Entry<String, S> entry : beans.entrySet()) {
				composite.addHealthIndicator(entry.getKey(),
						createHealthIndicator(entry.getValue()));
			}
			return composite;
		}

		@SuppressWarnings("unchecked")
		protected H createHealthIndicator(S source) {
			Class<?>[] generics = ResolvableType.forClass(
					CompositeHealthIndicatorConfiguration.class, getClass())
					.resolveGenerics();
			Class<H> indicatorClass = (Class<H>) generics[0];
			Class<S> sourceClass = (Class<S>) generics[1];
			try {
				return indicatorClass.getConstructor(sourceClass).newInstance(source);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to create indicator "
						+ indicatorClass + " for source " + sourceClass, ex);
			}
		}

	}

	@Configuration
	@ConditionalOnBean(DataSource.class)
	@ConditionalOnProperty(prefix = "management.health.db", name = "enabled", matchIfMissing = true)
	public static class DataSourcesHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<DataSourceHealthIndicator, DataSource>
			implements InitializingBean {

		@Autowired(required = false)
		private Map<String, DataSource> dataSources;

		@Autowired(required = false)
		private Collection<DataSourcePoolMetadataProvider> metadataProviders;

		private DataSourcePoolMetadataProvider poolMetadataProvider;

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
	@ConditionalOnProperty(prefix = "management.health.mongo", name = "enabled", matchIfMissing = true)
	public static class MongoHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<MongoHealthIndicator, MongoTemplate> {

		@Autowired
		private Map<String, MongoTemplate> mongoTemplates;

		@Bean
		@ConditionalOnMissingBean(name = "mongoHealthIndicator")
		public HealthIndicator mongoHealthIndicator() {
			return createHealthIndicator(this.mongoTemplates);
		}

	}

	@Configuration
	@ConditionalOnBean(RedisConnectionFactory.class)
	@ConditionalOnProperty(prefix = "management.health.redis", name = "enabled", matchIfMissing = true)
	public static class RedisHealthIndicatorConfiguration
			extends
			CompositeHealthIndicatorConfiguration<RedisHealthIndicator, RedisConnectionFactory> {

		@Autowired
		private Map<String, RedisConnectionFactory> redisConnectionFactories;

		@Bean
		@ConditionalOnMissingBean(name = "redisHealthIndicator")
		public HealthIndicator redisHealthIndicator() {
			return createHealthIndicator(this.redisConnectionFactories);
		}

	}

	@Configuration
	@ConditionalOnBean(RabbitTemplate.class)
	@ConditionalOnProperty(prefix = "management.health.rabbit", name = "enabled", matchIfMissing = true)
	public static class RabbitHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<RabbitHealthIndicator, RabbitTemplate> {

		@Autowired
		private Map<String, RabbitTemplate> rabbitTemplates;

		@Bean
		@ConditionalOnMissingBean(name = "rabbitHealthIndicator")
		public HealthIndicator rabbitHealthIndicator() {
			return createHealthIndicator(this.rabbitTemplates);
		}

	}

	@Configuration
	@ConditionalOnBean(SolrServer.class)
	@ConditionalOnProperty(prefix = "management.health.solr", name = "enabled", matchIfMissing = true)
	public static class SolrHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<SolrHealthIndicator, SolrServer> {

		@Autowired
		private Map<String, SolrServer> solrServers;

		@Bean
		@ConditionalOnMissingBean(name = "solrHealthIndicator")
		public HealthIndicator solrHealthIndicator() {
			return createHealthIndicator(this.solrServers);
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
	public static class MailHealthIndicatorConfiguration
			extends
			CompositeHealthIndicatorConfiguration<MailHealthIndicator, JavaMailSenderImpl> {

		@Autowired(required = false)
		private Map<String, JavaMailSenderImpl> mailSenders;

		@Bean
		@ConditionalOnMissingBean(name = "mailHealthIndicator")
		public HealthIndicator mailHealthIndicator() {
			return createHealthIndicator(this.mailSenders);
		}

	}

	@Configuration
	@ConditionalOnBean(ConnectionFactory.class)
	@ConditionalOnProperty(prefix = "management.health.jms", name = "enabled", matchIfMissing = true)
	public static class JmsHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<JmsHealthIndicator, ConnectionFactory> {

		@Autowired(required = false)
		private Map<String, ConnectionFactory> connectionFactories;

		@Bean
		@ConditionalOnMissingBean(name = "jmsHealthIndicator")
		public HealthIndicator jmsHealthIndicator() {
			return createHealthIndicator(this.connectionFactories);
		}

	}

}
