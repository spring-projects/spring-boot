/*
 * Copyright 2012-2023 the original author or authors.
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

import javax.net.ssl.SSLParameters;

import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisSslClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
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
class JedisConnectionConfiguration extends RedisConnectionConfiguration {

	/**
	 * Constructs a new JedisConnectionConfiguration with the specified RedisProperties,
	 * RedisStandaloneConfiguration provider, RedisSentinelConfiguration provider,
	 * RedisClusterConfiguration provider, RedisConnectionDetails, and SslBundles
	 * provider.
	 * @param properties the RedisProperties object containing the Redis connection
	 * properties
	 * @param standaloneConfigurationProvider the provider for
	 * RedisStandaloneConfiguration
	 * @param sentinelConfiguration the provider for RedisSentinelConfiguration
	 * @param clusterConfiguration the provider for RedisClusterConfiguration
	 * @param connectionDetails the RedisConnectionDetails object containing the Redis
	 * connection details
	 * @param sslBundles the provider for SslBundles
	 */
	JedisConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisStandaloneConfiguration> standaloneConfigurationProvider,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
			ObjectProvider<RedisClusterConfiguration> clusterConfiguration, RedisConnectionDetails connectionDetails,
			ObjectProvider<SslBundles> sslBundles) {
		super(properties, connectionDetails, standaloneConfigurationProvider, sentinelConfiguration,
				clusterConfiguration, sslBundles);
	}

	/**
	 * Creates a JedisConnectionFactory for Redis based on the specified threading
	 * configuration.
	 * @param builderCustomizers the customizers for the JedisClientConfigurationBuilder
	 * @return the JedisConnectionFactory
	 */
	@Bean
	@ConditionalOnThreading(Threading.PLATFORM)
	JedisConnectionFactory redisConnectionFactory(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		return createJedisConnectionFactory(builderCustomizers);
	}

	/**
	 * Creates a JedisConnectionFactory for virtual threading.
	 * @param builderCustomizers ObjectProvider of
	 * JedisClientConfigurationBuilderCustomizer
	 * @return JedisConnectionFactory for virtual threading
	 */
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

	/**
	 * Creates a JedisConnectionFactory based on the provided builder customizers.
	 * @param builderCustomizers the customizers for the JedisClientConfigurationBuilder
	 * @return a JedisConnectionFactory instance
	 */
	private JedisConnectionFactory createJedisConnectionFactory(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		JedisClientConfiguration clientConfiguration = getJedisClientConfiguration(builderCustomizers);
		if (getSentinelConfig() != null) {
			return new JedisConnectionFactory(getSentinelConfig(), clientConfiguration);
		}
		if (getClusterConfiguration() != null) {
			return new JedisConnectionFactory(getClusterConfiguration(), clientConfiguration);
		}
		return new JedisConnectionFactory(getStandaloneConfig(), clientConfiguration);
	}

	/**
	 * Returns the JedisClientConfiguration for the Jedis connection.
	 * @param builderCustomizers the customizers for the JedisClientConfiguration builder
	 * @return the JedisClientConfiguration
	 */
	private JedisClientConfiguration getJedisClientConfiguration(
			ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
		JedisClientConfigurationBuilder builder = applyProperties(JedisClientConfiguration.builder());
		if (isSslEnabled()) {
			applySsl(builder);
		}
		RedisProperties.Pool pool = getProperties().getJedis().getPool();
		if (isPoolEnabled(pool)) {
			applyPooling(pool, builder);
		}
		if (StringUtils.hasText(getProperties().getUrl())) {
			customizeConfigurationFromUrl(builder);
		}
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
	 * Applies the properties from the JedisConnectionConfiguration to the
	 * JedisClientConfigurationBuilder.
	 * @param builder the JedisClientConfigurationBuilder to apply the properties to
	 * @return the modified JedisClientConfigurationBuilder
	 */
	private JedisClientConfigurationBuilder applyProperties(JedisClientConfigurationBuilder builder) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(getProperties().getTimeout()).to(builder::readTimeout);
		map.from(getProperties().getConnectTimeout()).to(builder::connectTimeout);
		map.from(getProperties().getClientName()).whenHasText().to(builder::clientName);
		return builder;
	}

	/**
	 * Applies SSL configuration to the JedisClientConfigurationBuilder.
	 * @param builder The JedisClientConfigurationBuilder to apply SSL configuration to.
	 */
	private void applySsl(JedisClientConfigurationBuilder builder) {
		JedisSslClientConfigurationBuilder sslBuilder = builder.useSsl();
		if (getProperties().getSsl().getBundle() != null) {
			SslBundle sslBundle = getSslBundles().getBundle(getProperties().getSsl().getBundle());
			sslBuilder.sslSocketFactory(sslBundle.createSslContext().getSocketFactory());
			SslOptions sslOptions = sslBundle.getOptions();
			SSLParameters sslParameters = new SSLParameters();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(sslOptions.getCiphers()).to(sslParameters::setCipherSuites);
			map.from(sslOptions.getEnabledProtocols()).to(sslParameters::setProtocols);
			sslBuilder.sslParameters(sslParameters);
		}
	}

	/**
	 * Applies pooling configuration to the Jedis client configuration builder.
	 * @param pool the Redis pool configuration
	 * @param builder the Jedis client configuration builder
	 */
	private void applyPooling(RedisProperties.Pool pool,
			JedisClientConfiguration.JedisClientConfigurationBuilder builder) {
		builder.usePooling().poolConfig(jedisPoolConfig(pool));
	}

	/**
	 * Creates a JedisPoolConfig object based on the provided RedisProperties.Pool object.
	 * @param pool the RedisProperties.Pool object containing the pool configuration
	 * properties
	 * @return a JedisPoolConfig object with the configured properties
	 */
	private JedisPoolConfig jedisPoolConfig(RedisProperties.Pool pool) {
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

	/**
	 * Customizes the Jedis client configuration from a URL.
	 * @param builder the Jedis client configuration builder
	 */
	private void customizeConfigurationFromUrl(JedisClientConfiguration.JedisClientConfigurationBuilder builder) {
		if (urlUsesSsl()) {
			builder.useSsl();
		}
	}

}
