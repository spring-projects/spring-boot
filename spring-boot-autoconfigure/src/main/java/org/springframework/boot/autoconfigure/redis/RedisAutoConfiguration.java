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
import org.springframework.boot.context.properties.ConfigurationProperties;
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
public class RedisAutoConfiguration {

	@Configuration
	@ConditionalOnMissingClass(name = "org.apache.commons.pool.impl.GenericObjectPool")
	protected static class RedisConnectionConfiguration {

		@Autowired
		private RedisProperties config;

		@Bean
		@ConditionalOnMissingBean
		RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			LettuceConnectionFactory factory = new LettuceConnectionFactory(
					this.config.getHost(), this.config.getPort());
			if (this.config.getPassword() != null) {
				factory.setPassword(this.config.getPassword());
			}
			return factory;
		}

	}

	@Configuration
	@ConditionalOnClass(GenericObjectPool.class)
	protected static class RedisPooledConnectionConfiguration {

		@Autowired
		private RedisProperties config;

		@Bean
		@ConditionalOnMissingBean
		RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			if (this.config.getPool() != null) {
				LettuceConnectionFactory factory = new LettuceConnectionFactory(
						lettucePool());
				return factory;
			}
			LettuceConnectionFactory factory = new LettuceConnectionFactory(
					this.config.getHost(), this.config.getPort());
			if (this.config.getPassword() != null) {
				factory.setPassword(this.config.getPassword());
			}
			return factory;
		}

		@Bean
		@ConditionalOnMissingBean
		public LettucePool lettucePool() {
			return new DefaultLettucePool(this.config.getHost(), this.config.getPort(),
					poolConfig());
		}

		private PoolConfig poolConfig() {
			PoolConfig pool = new PoolConfig();
			RedisProperties.Pool props = this.config.getPool();
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
		private RedisProperties config;

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

	@ConfigurationProperties(name = "spring.redis")
	public static class RedisProperties {

		private String host = "localhost";

		private String password;

		private int port = 6379;

		private Pool pool;

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Pool getPool() {
			return this.pool;
		}

		public void setPool(Pool pool) {
			this.pool = pool;
		}

		public static class Pool {

			private int maxIdle = 8;

			private int minIdle = 0;

			private int maxActive = 8;

			private int maxWait = -1;

			public int getMaxIdle() {
				return this.maxIdle;
			}

			public void setMaxIdle(int maxIdle) {
				this.maxIdle = maxIdle;
			}

			public int getMinIdle() {
				return this.minIdle;
			}

			public void setMinIdle(int minIdle) {
				this.minIdle = minIdle;
			}

			public int getMaxActive() {
				return this.maxActive;
			}

			public void setMaxActive(int maxActive) {
				this.maxActive = maxActive;
			}

			public int getMaxWait() {
				return this.maxWait;
			}

			public void setMaxWait(int maxWait) {
				this.maxWait = maxWait;
			}
		}

	}

}
