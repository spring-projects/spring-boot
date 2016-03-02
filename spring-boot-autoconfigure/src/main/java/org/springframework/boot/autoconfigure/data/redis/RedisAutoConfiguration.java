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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Cluster;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce;
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
import org.springframework.data.redis.connection.lettuce.DefaultLettucePool;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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
 * @author Marco Aust
 * @author Mark Paluch
 */
@Configuration
@ConditionalOnClass({ RedisOperations.class })
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

	/**
	 * Jedis Redis connection configuration.
	 */
	@Configuration
	@ConditionalOnClass({ GenericObjectPool.class, JedisConnection.class, Jedis.class })
	protected static class JedisRedisConnectionConfiguration
			extends RedisBaseConfiguration {

		private final RedisProperties properties;

		public JedisRedisConnectionConfiguration(RedisProperties properties,
				ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
				ObjectProvider<RedisClusterConfiguration> clusterConfiguration) {
			super(properties, sentinelConfiguration, clusterConfiguration);
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(RedisConnectionFactory.class)
		public JedisConnectionFactory redisConnectionFactory()
				throws UnknownHostException {
			return applyProperties(createJedisConnectionFactory());
		}

		protected final JedisConnectionFactory applyProperties(
				JedisConnectionFactory factory) {
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
			String url = this.properties.getUrl();
			if (url.startsWith("rediss://")) {
				factory.setUseSsl(true);
			}
			try {
				URI uri = new URI(url);
				factory.setHostName(uri.getHost());
				factory.setPort(uri.getPort());
				if (uri.getUserInfo() != null) {
					String password = uri.getUserInfo();
					int index = password.lastIndexOf(":");
					if (index >= 0) {
						password = password.substring(index + 1);
					}
					factory.setPassword(password);
				}
			}
			catch (URISyntaxException ex) {
				throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url,
						ex);
			}
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
	 * Lettuce Redis connection configuration.
	 */
	@Configuration
	@ConditionalOnClass({ GenericObjectPool.class, RedisClient.class,
			RedisClusterClient.class })
	protected static class LettuceRedisConnectionConfiguration
			extends RedisBaseConfiguration {

		private final RedisProperties properties;

		public LettuceRedisConnectionConfiguration(RedisProperties properties,
				ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
				ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
			super(properties, sentinelConfigurationProvider,
					clusterConfigurationProvider);
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

		protected final LettuceConnectionFactory applyProperties(
				LettuceConnectionFactory factory) {
			configureConnection(factory);
			if (this.properties.isSsl()) {
				factory.setUseSsl(true);
			}
			if (this.properties.getLettuce() != null) {
				Lettuce lettuce = this.properties.getLettuce();
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
			String url = this.properties.getUrl();
			if (url.startsWith("rediss://")) {
				factory.setUseSsl(true);
			}
			try {
				URI uri = new URI(url);
				factory.setHostName(uri.getHost());
				factory.setPort(uri.getPort());
				if (uri.getUserInfo() != null) {
					String password = uri.getUserInfo();
					int index = password.lastIndexOf(":");
					if (index >= 0) {
						password = password.substring(index + 1);
					}
					factory.setPassword(password);
				}
			}
			catch (URISyntaxException ex) {
				throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url,
						ex);
			}
		}

		protected final DefaultLettucePool applyProperties(DefaultLettucePool pool) {
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
			String url = this.properties.getUrl();
			try {
				URI uri = new URI(url);
				lettucePool.setHostName(uri.getHost());
				lettucePool.setPort(uri.getPort());
				if (uri.getUserInfo() != null) {
					String password = uri.getUserInfo();
					int index = password.lastIndexOf(":");
					if (index >= 0) {
						password = password.substring(index + 1);
					}
					lettucePool.setPassword(password);
				}
			}
			catch (URISyntaxException ex) {
				throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url,
						ex);
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
				return applyClientResources(
						new LettuceConnectionFactory(getSentinelConfig()),
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
				return new LettuceConnectionFactory(applyProperties(
						applyClientResources(lettucePool, clientResources)));
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

	protected abstract static class RedisBaseConfiguration {

		private final RedisProperties properties;

		private final RedisSentinelConfiguration sentinelConfiguration;

		private final RedisClusterConfiguration clusterConfiguration;

		protected RedisBaseConfiguration(RedisProperties properties,
				ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
				ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
			this.properties = properties;
			this.sentinelConfiguration = sentinelConfigurationProvider.getIfAvailable();
			this.clusterConfiguration = clusterConfigurationProvider.getIfAvailable();
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
			List<RedisNode> nodes = new ArrayList<>();
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
			RedisTemplate<Object, Object> template = new RedisTemplate<>();
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
