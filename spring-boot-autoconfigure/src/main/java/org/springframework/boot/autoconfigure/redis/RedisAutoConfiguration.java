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

package org.springframework.boot.autoconfigure.redis;

import java.net.UnknownHostException;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Redis support.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass({ JedisConnection.class, RedisOperations.class, Jedis.class })
@EnableConfigurationProperties
public class RedisAutoConfiguration {

	@Bean(name = "org.springframework.autoconfigure.redis.RedisProperties")
	@ConditionalOnMissingBean
	public RedisProperties redisProperties() {
		return new RedisProperties();
	}

	@Configuration
	@ConditionalOnMissingClass(name = "org.apache.commons.pool2.impl.GenericObjectPool")
	protected static class RedisConnectionConfiguration {

		@Autowired
		private RedisProperties properties;

		@Bean
		@ConditionalOnMissingBean
		RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			JedisConnectionFactory factory = new JedisConnectionFactory();
			applyConnectionFactoryProperties(factory, this.properties);
			return factory;
		}

	}

	@Configuration
	@ConditionalOnClass(GenericObjectPool.class)
	protected static class RedisPooledConnectionConfiguration {

		@Autowired
		private RedisProperties properties;

		@Bean
		@ConditionalOnMissingBean
		RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			JedisConnectionFactory factory = createJedisConnectionFactory();
			applyConnectionFactoryProperties(factory, this.properties);
			return factory;
		}

		private JedisConnectionFactory createJedisConnectionFactory() {
			if (this.properties.getPool() != null) {
				return new JedisConnectionFactory(jedisPoolConfig());
			}
			return new JedisConnectionFactory();
		}

		private JedisPoolConfig jedisPoolConfig() {
			JedisPoolConfig config = new JedisPoolConfig();
			RedisProperties.Pool props = this.properties.getPool();
			config.setMaxTotal(props.getMaxActive());
			config.setMaxIdle(props.getMaxIdle());
			config.setMinIdle(props.getMinIdle());
			config.setMaxWaitMillis(props.getMaxWait());
			return config;
		}

	}

	@Configuration
	protected static class RedisConfiguration {

		@Autowired
		private RedisProperties properties;

		@Bean
		@ConditionalOnMissingBean(name = "redisTemplate")
		RedisOperations<Object, Object> redisTemplate(
				RedisConnectionFactory redisConnectionFactory)
				throws UnknownHostException {
			RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
			template.setConnectionFactory(redisConnectionFactory);
			return template;
		}

		@Bean
		@ConditionalOnMissingBean(StringRedisTemplate.class)
		StringRedisTemplate stringRedisTemplate(
				RedisConnectionFactory redisConnectionFactory)
				throws UnknownHostException {
			StringRedisTemplate template = new StringRedisTemplate();
			template.setConnectionFactory(redisConnectionFactory);
			return template;
		}

	}

	static void applyConnectionFactoryProperties(JedisConnectionFactory factory,
			RedisProperties properties) {
		factory.setHostName(properties.getHost());
		factory.setPort(properties.getPort());
		if (properties.getPassword() != null) {
			factory.setPassword(properties.getPassword());
		}
		factory.setDatabase(properties.getDatabase());
	}

}
