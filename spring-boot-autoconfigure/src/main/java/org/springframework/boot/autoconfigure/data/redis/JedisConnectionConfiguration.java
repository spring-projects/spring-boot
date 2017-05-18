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

import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.StringUtils;

/**
 * Redis connection configuration using Jedis.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass({ GenericObjectPool.class, JedisConnection.class, Jedis.class })
class JedisConnectionConfiguration extends RedisConnectionConfiguration {

	private final RedisProperties properties;

	JedisConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
			ObjectProvider<RedisClusterConfiguration> clusterConfiguration) {
		super(properties, sentinelConfiguration, clusterConfiguration);
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	public JedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
		return applyProperties(createJedisConnectionFactory());
	}

	private JedisConnectionFactory applyProperties(JedisConnectionFactory factory) {
		configureConnection(factory);
		if (this.properties.isSsl()) {
			factory.setUseSsl(true);
		}
		factory.setDatabase(this.properties.getDatabase());
		if (this.properties.getTimeout() > 0) {
			factory.setTimeout(this.properties.getTimeout());
		}
		return factory;
	}

	private void configureConnection(JedisConnectionFactory factory) {
		if (StringUtils.hasText(this.properties.getUrl())) {
			configureConnectionFromUrl(factory);
		}
		else {
			factory.setHostName(this.properties.getHost());
			factory.setPort(this.properties.getPort());
			if (this.properties.getPassword() != null) {
				factory.setPassword(this.properties.getPassword());
			}
		}
	}

	private void configureConnectionFromUrl(JedisConnectionFactory factory) {
		ConnectionInfo connectionInfo = parseUrl(this.properties.getUrl());
		factory.setUseSsl(connectionInfo.isUseSsl());
		factory.setHostName(connectionInfo.getHostName());
		factory.setPort(connectionInfo.getPort());
		if (connectionInfo.getPassword() != null) {
			factory.setPassword(connectionInfo.getPassword());
		}
	}

	private JedisConnectionFactory createJedisConnectionFactory() {
		RedisProperties.Pool pool = this.properties.getJedis().getPool();
		JedisPoolConfig poolConfig = pool != null ? jedisPoolConfig(pool)
				: new JedisPoolConfig();

		if (getSentinelConfig() != null) {
			return new JedisConnectionFactory(getSentinelConfig(), poolConfig);
		}
		if (getClusterConfiguration() != null) {
			return new JedisConnectionFactory(getClusterConfiguration(), poolConfig);
		}
		return new JedisConnectionFactory(poolConfig);
	}

	private JedisPoolConfig jedisPoolConfig(RedisProperties.Pool pool) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(pool.getMaxActive());
		config.setMaxIdle(pool.getMaxIdle());
		config.setMinIdle(pool.getMinIdle());
		config.setMaxWaitMillis(pool.getMaxWait());
		return config;
	}

}
