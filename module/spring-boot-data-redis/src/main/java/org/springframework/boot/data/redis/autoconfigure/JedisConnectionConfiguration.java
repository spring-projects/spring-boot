/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.redis.autoconfigure;

import javax.net.ssl.SSLParameters;

import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisSslClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Redis connection configuration using Jedis.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ GenericObjectPool.class, JedisConnection.class, Jedis.class })
@ConditionalOnMissingBean(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.data.redis.client-type", havingValue = "jedis", matchIfMissing = true)
class JedisConnectionConfiguration extends DataRedisConnectionConfiguration {

	JedisConnectionConfiguration(DataRedisProperties properties,
			ObjectProvider<RedisStandaloneConfiguration> standaloneConfigurationProvider,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
			ObjectProvider<RedisClusterConfiguration> clusterConfiguration,
			ObjectProvider<RedisStaticMasterReplicaConfiguration> masterReplicaConfiguration,
			DataRedisConnectionDetails connectionDetails) {
		super(properties, connectionDetails, standaloneConfigurationProvider, sentinelConfiguration,
				clusterConfiguration, masterReplicaConfiguration);
	}

	@Bean
	@ConditionalOnThreading(Threading.PLATFORM)
	JedisConnectionFactory redisConnectionFactory(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		return createJedisConnectionFactory(builderCustomizers);
	}

	@Bean
	@ConditionalOnThreading(Threading.VIRTUAL)
	JedisConnectionFactory redisConnectionFactoryVirtualThreads(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		JedisConnectionFactory factory = createJedisConnectionFactory(builderCustomizers);
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("redis-");
		executor.setVirtualThreads(true);
		factory.setExecutor(executor);
		return factory;
	}

	private JedisConnectionFactory createJedisConnectionFactory(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		JedisClientConfiguration clientConfiguration = getJedisClientConfiguration(builderCustomizers);
		return switch (this.mode) {
			case STANDALONE -> new JedisConnectionFactory(getStandaloneConfig(), clientConfiguration);
			case CLUSTER -> {
				RedisClusterConfiguration clusterConfiguration = getClusterConfiguration();
				Assert.state(clusterConfiguration != null, "'clusterConfiguration' must not be null");
				yield new JedisConnectionFactory(clusterConfiguration, clientConfiguration);
			}
			case SENTINEL -> {
				RedisSentinelConfiguration sentinelConfig = getSentinelConfig();
				Assert.state(sentinelConfig != null, "'sentinelConfig' must not be null");
				yield new JedisConnectionFactory(sentinelConfig, clientConfiguration);
			}
			case MASTER_REPLICA -> throw new IllegalStateException("'masterReplicaConfig' is not supported by Jedis");
		};
	}

	private JedisClientConfiguration getJedisClientConfiguration(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		JedisClientConfigurationBuilder builder = applyProperties(JedisClientConfiguration.builder());
		applySslIfNeeded(builder);
		DataRedisProperties.Pool pool = getProperties().getJedis().getPool();
		if (isPoolEnabled(pool)) {
			applyPooling(pool, builder);
		}
		String url = getProperties().getUrl();
		if (StringUtils.hasText(url)) {
			customizeConfigurationFromUrl(builder, url);
		}
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private JedisClientConfigurationBuilder applyProperties(JedisClientConfigurationBuilder builder) {
		PropertyMapper map = PropertyMapper.get();
		map.from(getProperties().getTimeout()).to(builder::readTimeout);
		map.from(getProperties().getConnectTimeout()).to(builder::connectTimeout);
		map.from(getProperties().getClientName()).whenHasText().to(builder::clientName);
		return builder;
	}

	private void applySslIfNeeded(JedisClientConfigurationBuilder builder) {
		SslBundle sslBundle = getSslBundle();
		if (sslBundle == null) {
			return;
		}
		JedisSslClientConfigurationBuilder sslBuilder = builder.useSsl();
		sslBuilder.sslSocketFactory(sslBundle.createSslContext().getSocketFactory());
		SslOptions sslOptions = sslBundle.getOptions();
		SSLParameters sslParameters = new SSLParameters();
		PropertyMapper map = PropertyMapper.get();
		map.from(sslOptions.getCiphers()).to(sslParameters::setCipherSuites);
		map.from(sslOptions.getEnabledProtocols()).to(sslParameters::setProtocols);
		sslBuilder.sslParameters(sslParameters);
	}

	private void applyPooling(DataRedisProperties.Pool pool,
			JedisClientConfiguration.JedisClientConfigurationBuilder builder) {
		builder.usePooling().poolConfig(jedisPoolConfig(pool));
	}

	private JedisPoolConfig jedisPoolConfig(DataRedisProperties.Pool pool) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(pool.getMaxActive());
		config.setMaxIdle(pool.getMaxIdle());
		config.setMinIdle(pool.getMinIdle());
		if (pool.getTimeBetweenEvictionRuns() != null) {
			config.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
		}
		if (pool.getMaxWait() != null) {
			config.setMaxWait(pool.getMaxWait());
		}
		return config;
	}

	private void customizeConfigurationFromUrl(JedisClientConfiguration.JedisClientConfigurationBuilder builder,
			String url) {
		if (urlUsesSsl(url)) {
			builder.useSsl();
		}
	}

}
