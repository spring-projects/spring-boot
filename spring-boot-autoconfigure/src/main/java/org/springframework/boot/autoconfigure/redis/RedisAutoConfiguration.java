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

import org.apache.commons.pool.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.PoolConfig;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.DefaultLettucePool;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePool;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.lambdaworks.redis.RedisClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Redis support.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ LettuceConnection.class, RedisOperations.class, RedisClient.class })
@EnableConfigurationProperties
public class RedisAutoConfiguration {

	@Configuration
	@ConditionalOnMissingClass(name = "org.apache.commons.pool.impl.GenericObjectPool")
	protected static class RedisConnectionConfiguration {

		@Autowired
		private RedisProperties properties;

		@Bean
		@ConditionalOnMissingBean
		RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			LettuceConnectionFactory factory = new LettuceConnectionFactory(
					this.properties.getHost(), this.properties.getPort());
			if (this.properties.getPassword() != null) {
				factory.setPassword(this.properties.getPassword());
			}
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
			if (this.properties.getPool() != null) {
				LettuceConnectionFactory factory = new LettuceConnectionFactory(
						lettucePool());
				return factory;
			}
			LettuceConnectionFactory factory = new LettuceConnectionFactory(
					this.properties.getHost(), this.properties.getPort());
			if (this.properties.getPassword() != null) {
				factory.setPassword(this.properties.getPassword());
			}
			return factory;
		}

		@Bean
		@ConditionalOnMissingBean
		public LettucePool lettucePool() {
			return new DefaultLettucePool(this.properties.getHost(),
					this.properties.getPort(), poolConfig());
		}

		private PoolConfig poolConfig() {
			PoolConfig pool = new PoolConfig();
			RedisProperties.Pool props = this.properties.getPool();
			if (props != null) {
				pool.setMaxActive(props.getMaxActive());
				pool.setMaxIdle(props.getMaxIdle());
				pool.setMinIdle(props.getMinIdle());
				pool.setMaxWait(props.getMaxWait());
			}
			return pool;
		}

	}

	@Bean(name = "org.springframework.autoconfigure.redis.RedisProperties")
	@ConditionalOnMissingBean
	public RedisProperties redisProperties() {

		return new RedisProperties();

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

}
