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

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.MongoHealthIndicator;
import org.springframework.boot.actuate.health.RabbitHealthIndicator;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.actuate.health.SimpleDataSourceHealthIndicator;
import org.springframework.boot.actuate.health.VanillaHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthIndicator}s.
 * 
 * @author Christian Dupuis
 * @since 1.1.0
 */
@Configuration
@AutoConfigureBefore({ EndpointAutoConfiguration.class })
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
		MongoDataAutoConfiguration.class, RedisAutoConfiguration.class,
		RabbitAutoConfiguration.class })
public class HealthIndicatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HealthIndicator.class)
	public HealthIndicator<?> statusHealthIndicator() {
		return new VanillaHealthIndicator();
	}

	@Configuration
	@ConditionalOnBean(DataSource.class)
	@ConditionalOnExpression("${health.db.enabled:true}")
	public static class DataSourcesHealthIndicatorConfiguration {

		@Autowired(required = false)
		private Map<String, DataSource> dataSources;

		@Bean
		@ConditionalOnMissingBean(name = "dbHealthIndicator")
		public HealthIndicator<? extends Object> dbHealthIndicator() {
			if (this.dataSources.size() == 1) {
				return new SimpleDataSourceHealthIndicator(this.dataSources.values()
						.iterator().next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator();
			for (Map.Entry<String, DataSource> entry : this.dataSources.entrySet()) {
				composite.addHealthIndicator(entry.getKey(),
						new SimpleDataSourceHealthIndicator(entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnBean(MongoTemplate.class)
	@ConditionalOnExpression("${health.mongo.enabled:true}")
	public static class MongoHealthIndicatorConfiguration {

		@Autowired
		private Map<String, MongoTemplate> mongoTemplates;

		@Bean
		@ConditionalOnMissingBean(name = "mongoHealthIndicator")
		public HealthIndicator<?> mongoHealthIndicator() {
			if (this.mongoTemplates.size() == 1) {
				return new MongoHealthIndicator(this.mongoTemplates.values().iterator()
						.next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator();
			for (Map.Entry<String, MongoTemplate> entry : this.mongoTemplates.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new MongoHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnBean(RedisConnectionFactory.class)
	@ConditionalOnExpression("${health.redis.enabled:true}")
	public static class RedisHealthIndicatorConfiguration {

		@Autowired
		private Map<String, RedisConnectionFactory> redisConnectionFactories;

		@Bean
		@ConditionalOnMissingBean(name = "redisHealthIndicator")
		public HealthIndicator<?> redisHealthIndicator() {
			if (this.redisConnectionFactories.size() == 1) {
				return new RedisHealthIndicator(this.redisConnectionFactories.values()
						.iterator().next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator();
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
	@ConditionalOnExpression("${health.rabbit.enabled:true}")
	public static class RabbitHealthIndicatorConfiguration {

		@Autowired
		private Map<String, RabbitTemplate> rabbitTemplates;

		@Bean
		@ConditionalOnMissingBean(name = "rabbitHealthIndicator")
		public HealthIndicator<?> rabbitHealthIndicator() {
			if (this.rabbitTemplates.size() == 1) {
				return new RabbitHealthIndicator(this.rabbitTemplates.values().iterator()
						.next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator();
			for (Map.Entry<String, RabbitTemplate> entry : this.rabbitTemplates
					.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new RabbitHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

}
