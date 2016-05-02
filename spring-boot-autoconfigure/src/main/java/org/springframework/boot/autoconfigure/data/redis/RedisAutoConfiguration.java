/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Cluster;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Redis support.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Christoph Strobl
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass({ JedisConnection.class, RedisOperations.class, Jedis.class })
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

	/**
	 * Redis connection configuration.
	 */
	@Configuration
	@ConditionalOnClass(GenericObjectPool.class)
	protected static class RedisConnectionConfiguration {

		private final RedisProperties properties;

		private final RedisSentinelConfiguration sentinelConfiguration;

		private final RedisClusterConfiguration clusterConfiguration;

		public RedisConnectionConfiguration(RedisProperties properties,
				ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
				ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
			this.properties = properties;
			this.sentinelConfiguration = sentinelConfigurationProvider.getIfAvailable();
			this.clusterConfiguration = clusterConfigurationProvider.getIfAvailable();
		}

		@Bean
		@ConditionalOnMissingBean(RedisConnectionFactory.class)
		public JedisConnectionFactory redisConnectionFactory()
				throws UnknownHostException {
			return applyProperties(createJedisConnectionFactory());
		}

		protected final JedisConnectionFactory applyProperties(
				JedisConnectionFactory factory) {
			factory.setHostName(this.properties.getHost());
			factory.setPort(this.properties.getPort());
			if (this.properties.getPassword() != null) {
				factory.setPassword(this.properties.getPassword());
			}
			factory.setDatabase(this.properties.getDatabase());
			if (this.properties.getTimeout() > 0) {
				factory.setTimeout(this.properties.getTimeout());
			}
			return factory;
		}

		protected final RedisSentinelConfiguration getSentinelConfig() {
			if (this.sentinelConfiguration != null) {
				return this.sentinelConfiguration;
			}
			Sentinel sentinelProperties = this.properties.getSentinel();
			if (sentinelProperties != null) {
				RedisSentinelConfiguration config = new RedisSentinelConfiguration();
				config.master(sentinelProperties.getMaster());
				config.setSentinels(createSentinels(sentinelProperties));
				return config;
			}
			return null;
		}

		/**
		 * Create a {@link RedisClusterConfiguration} if necessary.
		 * @return {@literal null} if no cluster settings are set.
		 */
		protected final RedisClusterConfiguration getClusterConfiguration() {
			if (this.clusterConfiguration != null) {
				return this.clusterConfiguration;
			}
			if (this.properties.getCluster() == null) {
				return null;
			}
			Cluster clusterProperties = this.properties.getCluster();
			RedisClusterConfiguration config = new RedisClusterConfiguration(
					clusterProperties.getNodes());

			if (clusterProperties.getMaxRedirects() != null) {
				config.setMaxRedirects(clusterProperties.getMaxRedirects());
			}
			return config;
		}

		private List<RedisNode> createSentinels(Sentinel sentinel) {
			List<RedisNode> nodes = new ArrayList<RedisNode>();
			for (String node : StringUtils
					.commaDelimitedListToStringArray(sentinel.getNodes())) {
				try {
					String[] parts = StringUtils.split(node, ":");
					Assert.state(parts.length == 2, "Must be defined as 'host:port'");
					nodes.add(new RedisNode(parts[0], Integer.valueOf(parts[1])));
				}
				catch (RuntimeException ex) {
					throw new IllegalStateException(
							"Invalid redis sentinel " + "property '" + node + "'", ex);
				}
			}
			return nodes;
		}

		private JedisConnectionFactory createJedisConnectionFactory() {
			JedisPoolConfig poolConfig = this.properties.getPool() != null
					? jedisPoolConfig() : new JedisPoolConfig();

			if (getSentinelConfig() != null) {
				return new JedisConnectionFactory(getSentinelConfig(), poolConfig);
			}
			if (getClusterConfiguration() != null) {
				return new JedisConnectionFactory(getClusterConfiguration(), poolConfig);
			}
			return new JedisConnectionFactory(poolConfig);
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

	/**
	 * Standard Redis configuration.
	 */
	@Configuration
	protected static class RedisConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "redisTemplate")
		public RedisTemplate<Object, Object> redisTemplate(
				RedisConnectionFactory redisConnectionFactory)
						throws UnknownHostException {
			RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
			template.setConnectionFactory(redisConnectionFactory);
			return template;
		}

		@Bean
		@ConditionalOnMissingBean(StringRedisTemplate.class)
		public StringRedisTemplate stringRedisTemplate(
				RedisConnectionFactory redisConnectionFactory)
						throws UnknownHostException {
			StringRedisTemplate template = new StringRedisTemplate();
			template.setConnectionFactory(redisConnectionFactory);
			return template;
		}

	}

}
