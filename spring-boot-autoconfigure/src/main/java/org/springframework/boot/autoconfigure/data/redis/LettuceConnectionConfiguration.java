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

	LettuceConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
		super(properties, sentinelConfigurationProvider, clusterConfigurationProvider);
		this.properties = properties;
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
		return applyProperties(createLettuceConnectionFactory(clientResources));
	}

	private LettuceConnectionFactory applyProperties(LettuceConnectionFactory factory) {
		configureConnection(factory);
		if (this.properties.isSsl()) {
			factory.setUseSsl(true);
		}
		if (this.properties.getLettuce() != null) {
			RedisProperties.Lettuce lettuce = this.properties.getLettuce();
			if (lettuce.getShutdownTimeout() >= 0) {
				factory.setShutdownTimeout(
						this.properties.getLettuce().getShutdownTimeout());
			}
		}
		return factory;
	}

	private void configureConnection(LettuceConnectionFactory factory) {
		if (StringUtils.hasText(this.properties.getUrl())) {
			configureConnectionFromUrl(factory);
		}
		else {
			factory.setHostName(this.properties.getHost());
			factory.setPort(this.properties.getPort());
			if (this.properties.getPassword() != null) {
				factory.setPassword(this.properties.getPassword());
			}
			factory.setDatabase(this.properties.getDatabase());
			if (this.properties.getTimeout() > 0) {
				factory.setTimeout(this.properties.getTimeout());
			}
		}
	}

	private void configureConnectionFromUrl(LettuceConnectionFactory factory) {
		ConnectionInfo connectionInfo = parseUrl(this.properties.getUrl());
		factory.setUseSsl(connectionInfo.isUseSsl());
		factory.setHostName(connectionInfo.getHostName());
		factory.setPort(connectionInfo.getPort());
		if (connectionInfo.getPassword() != null) {
			factory.setPassword(connectionInfo.getPassword());
		}
	}

	private DefaultLettucePool applyProperties(DefaultLettucePool pool) {
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

	private LettuceConnectionFactory createLettuceConnectionFactory(
			ClientResources clientResources) {

		if (getSentinelConfig() != null) {
			if (this.properties.getLettuce() != null
					&& this.properties.getLettuce().getPool() != null) {
				DefaultLettucePool lettucePool = new DefaultLettucePool(
						getSentinelConfig());
				return new LettuceConnectionFactory(applyProperties(
						applyClientResources(lettucePool, clientResources)));
			}
			return applyClientResources(new LettuceConnectionFactory(getSentinelConfig()),
					clientResources);
		}

		if (getClusterConfiguration() != null) {
			return applyClientResources(
					new LettuceConnectionFactory(getClusterConfiguration()),
					clientResources);
		}

		if (this.properties.getLettuce() != null
				&& this.properties.getLettuce().getPool() != null) {
			GenericObjectPoolConfig config = lettucePoolConfig(
					this.properties.getLettuce().getPool());
			DefaultLettucePool lettucePool = new DefaultLettucePool(
					this.properties.getHost(), this.properties.getPort(), config);
			return new LettuceConnectionFactory(
					applyProperties(applyClientResources(lettucePool, clientResources)));
		}

		return applyClientResources(new LettuceConnectionFactory(), clientResources);
	}

	private DefaultLettucePool applyClientResources(DefaultLettucePool lettucePool,
			ClientResources clientResources) {
		lettucePool.setClientResources(clientResources);
		return lettucePool;
	}

	private LettuceConnectionFactory applyClientResources(
			LettuceConnectionFactory factory, ClientResources clientResources) {
		factory.setClientResources(clientResources);
		return factory;
	}

	private GenericObjectPoolConfig lettucePoolConfig(RedisProperties.Pool props) {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(props.getMaxActive());
		config.setMaxIdle(props.getMaxIdle());
		config.setMinIdle(props.getMinIdle());
		config.setMaxWaitMillis(props.getMaxWait());
		return config;
	}

}
