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

package org.springframework.boot.autoconfigure.data.redis;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.DefaultLettucePool;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

/**
 * Redis connection configuration using Lettuce.
 *
 * @author Mark Paluch
 */
@Configuration
@ConditionalOnClass({ GenericObjectPool.class, RedisClient.class,
		RedisClusterClient.class })
class LettuceConnectionConfiguration extends RedisConnectionConfiguration {

	private final RedisProperties properties;

	private final List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers;

	LettuceConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider,
			ObjectProvider<List<LettuceClientConfigurationBuilderCustomizer>> builderCustomizers) {
		super(properties, sentinelConfigurationProvider, clusterConfigurationProvider);
		this.properties = properties;
		this.builderCustomizers = builderCustomizers
				.getIfAvailable(Collections::emptyList);
	}

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(ClientResources.class)
	public DefaultClientResources lettuceClientResources() {
		return DefaultClientResources.create();
	}

	@Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	public LettuceConnectionFactory redisConnectionFactory(
			ClientResources clientResources) throws UnknownHostException {
		if (this.properties.getLettuce().getPool() != null) {
			return createLettuceConnectionFactory(clientResources);
		}
		return createLettuceConnectionFactory(
				getLettuceClientConfiguration(clientResources));
	}

	private LettuceConnectionFactory createLettuceConnectionFactory(
			ClientResources clientResources) {
		return applyProperties(
				new LettuceConnectionFactory(applyProperties(createLettucePool(),
						this.properties.getLettuce().getPool(), clientResources)));
	}

	@SuppressWarnings("deprecation")
	private LettuceConnectionFactory applyProperties(
			LettuceConnectionFactory connectionFactory) {
		connectionFactory
				.setShutdownTimeout(this.properties.getLettuce().getShutdownTimeout());
		return connectionFactory;
	}

	private DefaultLettucePool createLettucePool() {
		return getSentinelConfig() != null ? new DefaultLettucePool(getSentinelConfig())
				: new DefaultLettucePool();
	}

	private DefaultLettucePool applyProperties(DefaultLettucePool pool,
			RedisProperties.Pool properties, ClientResources clientResources) {
		if (StringUtils.hasText(this.properties.getUrl())) {
			configureConnectionFromUrl(pool);
		}
		else {
			pool.setHostName(this.properties.getHost());
			pool.setPort(this.properties.getPort());
			if (this.properties.getPassword() != null) {
				pool.setPassword(this.properties.getPassword());
			}
			pool.setDatabase(this.properties.getDatabase());
		}
		if (this.properties.getTimeout() > 0) {
			pool.setTimeout(this.properties.getTimeout());
		}
		pool.setPoolConfig(lettucePoolConfig(properties));
		pool.setClientResources(clientResources);
		pool.afterPropertiesSet();
		return pool;
	}

	private void configureConnectionFromUrl(DefaultLettucePool lettucePool) {
		ConnectionInfo connectionInfo = parseUrl(this.properties.getUrl());
		lettucePool.setHostName(connectionInfo.getHostName());
		lettucePool.setPort(connectionInfo.getPort());
		if (connectionInfo.getPassword() != null) {
			lettucePool.setPassword(connectionInfo.getPassword());
		}
	}

	private static GenericObjectPoolConfig lettucePoolConfig(RedisProperties.Pool props) {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(props.getMaxActive());
		config.setMaxIdle(props.getMaxIdle());
		config.setMinIdle(props.getMinIdle());
		config.setMaxWaitMillis(props.getMaxWait());
		return config;
	}

	private LettuceConnectionFactory createLettuceConnectionFactory(
			LettuceClientConfiguration clientConfiguration) {
		if (getSentinelConfig() != null) {
			return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
		}
		if (getClusterConfiguration() != null) {
			return new LettuceConnectionFactory(getClusterConfiguration(),
					clientConfiguration);
		}
		return new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
	}

	private LettuceClientConfiguration getLettuceClientConfiguration(
			ClientResources clientResources) {
		LettuceClientConfigurationBuilder builder = applyProperties(
				LettuceClientConfiguration.builder());
		if (StringUtils.hasText(this.properties.getUrl())) {
			customizeConfigurationFromUrl(builder);
		}
		builder.clientResources(clientResources);
		customize(builder);
		return builder.build();
	}

	private LettuceClientConfigurationBuilder applyProperties(
			LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		if (this.properties.isSsl()) {
			builder.useSsl();
		}
		if (this.properties.getTimeout() != 0) {
			builder.commandTimeout(Duration.ofMillis(this.properties.getTimeout()));
		}
		if (this.properties.getLettuce() != null) {
			RedisProperties.Lettuce lettuce = this.properties.getLettuce();
			if (lettuce.getShutdownTimeout() >= 0) {
				builder.shutdownTimeout(Duration
						.ofMillis(this.properties.getLettuce().getShutdownTimeout()));
			}
		}
		return builder;
	}

	private void customizeConfigurationFromUrl(
			LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		ConnectionInfo connectionInfo = parseUrl(this.properties.getUrl());
		if (connectionInfo.isUseSsl()) {
			builder.useSsl();
		}
	}

	private void customize(
			LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		for (LettuceClientConfigurationBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
	}

}
