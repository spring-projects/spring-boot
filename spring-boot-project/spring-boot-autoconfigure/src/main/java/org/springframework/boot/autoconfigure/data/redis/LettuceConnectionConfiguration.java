/*
 * Copyright 2012-2021 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.Builder;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce.Cluster.Refresh;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisSocketConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Redis connection configuration using Lettuce.
 *
 * @author Mark Paluch
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisClient.class)
@ConditionalOnProperty(name = "spring.redis.client-type", havingValue = "lettuce", matchIfMissing = true)
class LettuceConnectionConfiguration extends RedisConnectionConfiguration {

	private final RedisSocketConfiguration socketConfiguration;

	LettuceConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisStandaloneConfiguration> standaloneConfigurationProvider,
			ObjectProvider<RedisSocketConfiguration> socketConfigurationProvider,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
		super(properties, standaloneConfigurationProvider, sentinelConfigurationProvider, clusterConfigurationProvider);
		this.socketConfiguration = socketConfigurationProvider.getIfAvailable();
	}

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(ClientResources.class)
	DefaultClientResources lettuceClientResources(ObjectProvider<ClientResourcesBuilderCustomizer> customizers) {
		DefaultClientResources.Builder builder = DefaultClientResources.builder();
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	LettuceConnectionFactory redisConnectionFactory(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources) {
		LettuceClientConfiguration clientConfig = getLettuceClientConfiguration(builderCustomizers, clientResources,
				getProperties().getLettuce().getPool());
		return createLettuceConnectionFactory(clientConfig);
	}

	private LettuceConnectionFactory createLettuceConnectionFactory(LettuceClientConfiguration clientConfiguration) {
		if (getSentinelConfig() != null) {
			return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
		}
		if (getClusterConfiguration() != null) {
			return new LettuceConnectionFactory(getClusterConfiguration(), clientConfiguration);
		}
		if (getSocketConfiguration() != null) {
			return new LettuceConnectionFactory(getSocketConfiguration(), clientConfiguration);
		}
		return new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
	}

	@Override
	RedisSentinelConfiguration maybeGetCustomSentinelConfig() {
		if (urlConfiguredForSentinelScheme(this.getProperties().getUrl())) {
			RedisURI redisURI = createRedisUri(this.getProperties().getUrl());
			return (RedisSentinelConfiguration) LettuceConnectionFactory.createRedisConfiguration(redisURI);
		}
		return null;
	}

	@Override
	RedisStandaloneConfiguration maybeGetCustomStandaloneConfig() {
		if (urlConfiguredForNonStandardStandaloneScheme(this.getProperties().getUrl())) {
			RedisURI redisURI = createRedisUri(this.getProperties().getUrl());
			return (RedisStandaloneConfiguration) LettuceConnectionFactory.createRedisConfiguration(redisURI);
		}
		return null;
	}

	private RedisSocketConfiguration getSocketConfiguration() {
		if (this.socketConfiguration != null) {
			return this.socketConfiguration;
		}
		if (urlConfiguredForSocketScheme(this.getProperties().getUrl())) {
			RedisURI redisURI = createRedisUri(this.getProperties().getUrl());
			return (RedisSocketConfiguration) LettuceConnectionFactory.createRedisConfiguration(redisURI);
		}
		return null;
	}

	private boolean urlConfiguredForSentinelScheme(String url) {
		String scheme = getUrlScheme(url);
		return scheme != null && RedisURI.URI_SCHEME_REDIS_SENTINEL.equals(scheme)
				|| RedisURI.URI_SCHEME_REDIS_SENTINEL_SECURE.equals(scheme);
	}

	private boolean urlConfiguredForSocketScheme(String url) {
		String scheme = getUrlScheme(url);
		return scheme != null && (RedisURI.URI_SCHEME_REDIS_SOCKET.equals(scheme)
				|| RedisURI.URI_SCHEME_REDIS_SOCKET_ALT.equals(scheme));
	}

	private boolean urlConfiguredForNonStandardStandaloneScheme(String url) {
		String scheme = getUrlScheme(url);
		return scheme != null && !RedisURI.URI_SCHEME_REDIS.equals(scheme)
				&& !RedisURI.URI_SCHEME_REDIS_SECURE.equals(scheme);
	}

	private String getUrlScheme(String url) {
		if (!StringUtils.hasText(url)) {
			return null;
		}
		try {
			URI uri = new URI(url);
			return uri.getScheme();
		}
		catch (URISyntaxException ex) {
			throw new RedisUrlSyntaxException(url, ex);
		}
	}

	private LettuceClientConfiguration getLettuceClientConfiguration(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources, Pool pool) {
		LettuceClientConfigurationBuilder builder = createBuilder(pool);
		applyProperties(builder);
		if (StringUtils.hasText(getProperties().getUrl())) {
			customizeConfigurationFromUrl(builder);
		}
		builder.clientOptions(createClientOptions());
		builder.clientResources(clientResources);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private LettuceClientConfigurationBuilder createBuilder(Pool pool) {
		if (isPoolEnabled(pool)) {
			return new PoolBuilderFactory().createBuilder(pool);
		}
		return LettuceClientConfiguration.builder();
	}

	private LettuceClientConfigurationBuilder applyProperties(
			LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		if (getProperties().isSsl()) {
			builder.useSsl();
		}
		if (getProperties().getTimeout() != null) {
			builder.commandTimeout(getProperties().getTimeout());
		}
		if (getProperties().getLettuce() != null) {
			RedisProperties.Lettuce lettuce = getProperties().getLettuce();
			if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
				builder.shutdownTimeout(getProperties().getLettuce().getShutdownTimeout());
			}
		}
		if (StringUtils.hasText(getProperties().getClientName())) {
			builder.clientName(getProperties().getClientName());
		}
		return builder;
	}

	private ClientOptions createClientOptions() {
		ClientOptions.Builder builder = initializeClientOptionsBuilder();
		Duration connectTimeout = getProperties().getConnectTimeout();
		if (connectTimeout != null) {
			builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
		}
		return builder.timeoutOptions(TimeoutOptions.enabled()).build();
	}

	private ClientOptions.Builder initializeClientOptionsBuilder() {
		if (getProperties().getCluster() != null) {
			ClusterClientOptions.Builder builder = ClusterClientOptions.builder();
			Refresh refreshProperties = getProperties().getLettuce().getCluster().getRefresh();
			Builder refreshBuilder = ClusterTopologyRefreshOptions.builder()
					.dynamicRefreshSources(refreshProperties.isDynamicRefreshSources());
			if (refreshProperties.getPeriod() != null) {
				refreshBuilder.enablePeriodicRefresh(refreshProperties.getPeriod());
			}
			if (refreshProperties.isAdaptive()) {
				refreshBuilder.enableAllAdaptiveRefreshTriggers();
			}
			return builder.topologyRefreshOptions(refreshBuilder.build());
		}
		return ClientOptions.builder();
	}

	private void customizeConfigurationFromUrl(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		RedisURI redisURI = createRedisUri(getProperties().getUrl());
		builder.apply(redisURI);
	}

	private RedisURI createRedisUri(String uri) {
		RedisURI redisURI = RedisURI.create(uri);
		// Set the sentinel password from properties on the sentinel nodes as there is no
		// way to set that on the url
		if (!ObjectUtils.isEmpty(redisURI.getSentinels()) && getProperties().getSentinel() != null
				&& StringUtils.hasText(getProperties().getSentinel().getPassword())) {
			redisURI.getSentinels().forEach((sentinelNodeRedisUri) -> sentinelNodeRedisUri
					.setPassword(getProperties().getSentinel().getPassword().toCharArray()));
		}
		return redisURI;
	}

	/**
	 * Inner class to allow optional commons-pool2 dependency.
	 */
	private static class PoolBuilderFactory {

		LettuceClientConfigurationBuilder createBuilder(Pool properties) {
			return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(properties));
		}

		private GenericObjectPoolConfig<?> getPoolConfig(Pool properties) {
			GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
			config.setMaxTotal(properties.getMaxActive());
			config.setMaxIdle(properties.getMaxIdle());
			config.setMinIdle(properties.getMinIdle());
			if (properties.getTimeBetweenEvictionRuns() != null) {
				config.setTimeBetweenEvictionRuns(properties.getTimeBetweenEvictionRuns());
			}
			if (properties.getMaxWait() != null) {
				config.setMaxWait(properties.getMaxWait());
			}
			return config;
		}

	}

}
