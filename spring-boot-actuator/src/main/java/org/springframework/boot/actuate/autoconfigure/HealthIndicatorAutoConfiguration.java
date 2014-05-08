/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.MongoHealthIndicator;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.actuate.health.SimpleHealthIndicator;
import org.springframework.boot.actuate.health.VanillaHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.CommonsDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.HikariDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.TomcatDataSourceConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author Christian Dupuis
 * @since 1.1.0
 */
@Configuration
@AutoConfigureAfter({ DataSourceAutoConfiguration.class,
		EmbeddedDataSourceConfiguration.class, CommonsDataSourceConfiguration.class,
		HikariDataSourceConfiguration.class, TomcatDataSourceConfiguration.class,
		MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
		RedisAutoConfiguration.class })
public class HealthIndicatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HealthIndicator.class)
	public HealthIndicator<?> statusHealthIndicator() {
		return new VanillaHealthIndicator();
	}

	@Configuration
	@ConditionalOnBean(DataSource.class)
	public static class DataSourcesHealthIndicatorConfiguration {

		@Autowired(required = false)
		private Map<String, DataSource> dataSources;

		@Bean
		@ConditionalOnMissingBean(name = "dbHealthIndicator")
		public HealthIndicator<? extends Object> dbHealthIndicator() {
			if (this.dataSources.size() == 1) {
				return new SimpleHealthIndicator(this.dataSources.values().iterator()
						.next());
			}

			CompositeHealthIndicator composite = new CompositeHealthIndicator();
			for (Map.Entry<String, DataSource> entry : this.dataSources.entrySet()) {
				composite.addHealthIndicator(entry.getKey(), new SimpleHealthIndicator(
						entry.getValue()));
			}
			return composite;
		}
	}

	@Configuration
	@ConditionalOnBean(MongoTemplate.class)
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

}
