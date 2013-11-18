/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's redis support.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ LettuceConnection.class, RedisOperations.class })
public class RedisAutoConfiguration {

	@Configuration
	@EnableConfigurationProperties(RedisProperties.class)
	protected static class RedisConfiguration {

		@Autowired
		private RedisProperties config;

		@Bean
		@ConditionalOnMissingBean(RedisConnectionFactory.class)
		RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			LettuceConnectionFactory factory = new LettuceConnectionFactory(
					this.config.getHost(), this.config.getPort());
			if (this.config.getPassword() != null) {
				factory.setPassword(this.config.getPassword());
			}
			return factory;
		}

		@Bean
		@ConditionalOnMissingBean(RedisOperations.class)
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

	@ConfigurationProperties(name = "spring.data.redis")
	public static class RedisProperties {

		private String host = "localhost";

		private String password;

		private int port = 6379;

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

	}

}
