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

package org.springframework.boot.autoconfigure.data.redis;

import java.net.UnknownHostException;

import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

/**
 * Redis connection configuration using Lettuce.
 *
 * @author Mark Paluch
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisClient.class)
class LettuceConnectionConfiguration extends RedisConnectionConfiguration {

	LettuceConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
		super(properties, sentinelConfigurationProvider, clusterConfigurationProvider);
	}

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(ClientResources.class)
	DefaultClientResources lettuceClientResources() {
		return DefaultClientResources.create();
	}

	@Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	LettuceConnectionFactory redisConnectionFactory(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources) throws UnknownHostException {
		LettuceClientConfiguration clientConfig = getLettuceClientConfiguration(builderCustomizers, clientResources,
				getProperties().getLettuce().getPool());
		return createLettuceConnectionFactory(clientConfig);
	}

	private LettuceConnectionFactory createLettuceConnectionFactory(LettuceClientConfiguration clientConfiguration) {
		if (getSentinelConfig() != null) {
			return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
		}
		if (getClusterConfiguration() != null) {
			return new LettuceConnectionFactory(getClusterConfiguration(), clientConfiguration);
		}
		return new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
	}

	private LettuceClientConfiguration getLettuceClientConfiguration(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources, Pool pool) {
		LettuceClientConfigurationBuilder builder = createBuilder(pool);
		applyProperties(builder);
		if (StringUtils.hasText(getProperties().getUrl())) {
			customizeConfigurationFromUrl(builder);
		}
		builder.clientResources(clientResources);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private LettuceClientConfigurationBuilder createBuilder(Pool pool) {
		if (pool == null) {
			return LettuceClientConfiguration.builder();
		}
		return new PoolBuilderFactory().createBuilder(pool);
	}

	private LettuceClientConfigurationBuilder applyProperties(
			LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		if (getProperties().isSsl()) {
			builder.useSsl();
		}
		if (getProperties().getTimeout() != null) {
			builder.commandTimeout(getProperties().getTimeout());
		}
		if (getProperties().getLettuce() != null) {
			RedisProperties.Lettuce lettuce = getProperties().getLettuce();
			if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
				builder.shutdownTimeout(getProperties().getLettuce().getShutdownTimeout());
			}
		}
		if (StringUtils.hasText(getProperties().getClientName())) {
			builder.clientName(getProperties().getClientName());
		}
		return builder;
	}

	private void customizeConfigurationFromUrl(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		ConnectionInfo connectionInfo = parseUrl(getProperties().getUrl());
		if (connectionInfo.isUseSsl()) {
			builder.useSsl();
		}
	}

	/**
	 * Inner class to allow optional commons-pool2 dependency.
	 */
	private static class PoolBuilderFactory {

		LettuceClientConfigurationBuilder createBuilder(Pool properties) {
			return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(properties));
		}

		private GenericObjectPoolConfig<?> getPoolConfig(Pool properties) {
			GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
			config.setMaxTotal(properties.getMaxActive());
			config.setMaxIdle(properties.getMaxIdle());
			config.setMinIdle(properties.getMinIdle());
			if (properties.getTimeBetweenEvictionRuns() != null) {
				config.setTimeBetweenEvictionRunsMillis(properties.getTimeBetweenEvictionRuns().toMillis());
			}
			if (properties.getMaxWait() != null) {
				config.setMaxWaitMillis(properties.getMaxWait().toMillis());
			}
			return config;
		}

	}

}
