/*
 * Copyright 2012-2024 the original author or authors.
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

import java.time.Duration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce.Cluster.Refresh;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.boot.autoconfigure.thread.Threading;
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
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

/**
 * Redis connection configuration using Lettuce.
 *
 * @author Mark Paluch
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author Scott Frederick
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisClient.class)
@ConditionalOnProperty(name = "spring.data.redis.client-type", havingValue = "lettuce", matchIfMissing = true)
class LettuceConnectionConfiguration extends RedisConnectionConfiguration {

	/**
     * Constructs a new LettuceConnectionConfiguration with the specified RedisProperties, RedisStandaloneConfiguration provider,
     * RedisSentinelConfiguration provider, RedisClusterConfiguration provider, RedisConnectionDetails, and SslBundles provider.
     * 
     * @param properties the RedisProperties object containing the configuration properties for Redis
     * @param standaloneConfigurationProvider the provider for RedisStandaloneConfiguration
     * @param sentinelConfigurationProvider the provider for RedisSentinelConfiguration
     * @param clusterConfigurationProvider the provider for RedisClusterConfiguration
     * @param connectionDetails the RedisConnectionDetails object containing the connection details for Redis
     * @param sslBundles the provider for SslBundles
     */
    LettuceConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisStandaloneConfiguration> standaloneConfigurationProvider,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider,
			RedisConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
		super(properties, connectionDetails, standaloneConfigurationProvider, sentinelConfigurationProvider,
				clusterConfigurationProvider, sslBundles);
	}

	/**
     * Creates and configures the default client resources for Lettuce connection.
     * If no custom client resources are provided, a default instance will be created.
     * 
     * @param customizers the customizers for the client resources builder
     * @return the configured default client resources
     */
    @Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(ClientResources.class)
	DefaultClientResources lettuceClientResources(ObjectProvider<ClientResourcesBuilderCustomizer> customizers) {
		DefaultClientResources.Builder builder = DefaultClientResources.builder();
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
     * Creates a RedisConnectionFactory using Lettuce library if no RedisConnectionFactory bean is already present.
     * This method is conditionally executed only if the Threading mode is set to PLATFORM.
     * 
     * @param builderCustomizers ObjectProvider of LettuceClientConfigurationBuilderCustomizer to customize the Lettuce client configuration builder
     * @param clientResources ClientResources to be used by the Lettuce client
     * @return RedisConnectionFactory created using Lettuce library
     */
    @Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	@ConditionalOnThreading(Threading.PLATFORM)
	LettuceConnectionFactory redisConnectionFactory(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources) {
		return createConnectionFactory(builderCustomizers, clientResources);
	}

	/**
     * Creates a LettuceConnectionFactory bean for virtual threading.
     * This bean is conditional on the absence of a RedisConnectionFactory bean and the use of virtual threading.
     * It uses the LettuceClientConfigurationBuilderCustomizer and ClientResources beans provided by the ObjectProvider.
     * It creates a LettuceConnectionFactory with a SimpleAsyncTaskExecutor configured for virtual threading.
     * 
     * @param builderCustomizers The ObjectProvider for LettuceClientConfigurationBuilderCustomizer beans.
     * @param clientResources The ClientResources bean.
     * @return The LettuceConnectionFactory bean for virtual threading.
     */
    @Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	@ConditionalOnThreading(Threading.VIRTUAL)
	LettuceConnectionFactory redisConnectionFactoryVirtualThreads(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources) {
		LettuceConnectionFactory factory = createConnectionFactory(builderCustomizers, clientResources);
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("redis-");
		executor.setVirtualThreads(true);
		factory.setExecutor(executor);
		return factory;
	}

	/**
     * Creates a LettuceConnectionFactory with the given builder customizers and client resources.
     * 
     * @param builderCustomizers the object provider for LettuceClientConfigurationBuilderCustomizer instances
     * @param clientResources the client resources for the connection factory
     * @return a LettuceConnectionFactory instance
     */
    private LettuceConnectionFactory createConnectionFactory(
			ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
			ClientResources clientResources) {
		LettuceClientConfiguration clientConfig = getLettuceClientConfiguration(builderCustomizers, clientResources,
				getProperties().getLettuce().getPool());
		return createLettuceConnectionFactory(clientConfig);
	}

	/**
     * Creates a LettuceConnectionFactory based on the provided client configuration.
     * If a sentinel configuration is available, a LettuceConnectionFactory with the sentinel configuration and client configuration is created.
     * If a cluster configuration is available, a LettuceConnectionFactory with the cluster configuration and client configuration is created.
     * If neither sentinel nor cluster configuration is available, a LettuceConnectionFactory with the standalone configuration and client configuration is created.
     *
     * @param clientConfiguration the client configuration to be used for creating the LettuceConnectionFactory
     * @return a LettuceConnectionFactory based on the provided client configuration
     */
    private LettuceConnectionFactory createLettuceConnectionFactory(LettuceClientConfiguration clientConfiguration) {
		if (getSentinelConfig() != null) {
			return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
		}
		if (getClusterConfiguration() != null) {
			return new LettuceConnectionFactory(getClusterConfiguration(), clientConfiguration);
		}
		return new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
	}

	/**
     * Returns the LettuceClientConfiguration for the Lettuce connection.
     * 
     * @param builderCustomizers ObjectProvider of LettuceClientConfigurationBuilderCustomizer to customize the LettuceClientConfigurationBuilder
     * @param clientResources ClientResources for the Lettuce connection
     * @param pool Pool for the Lettuce connection
     * @return LettuceClientConfiguration for the Lettuce connection
     */
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

	/**
     * Creates a LettuceClientConfigurationBuilder based on the provided Pool.
     * If the Pool is enabled, it uses a PoolBuilderFactory to create the builder.
     * Otherwise, it creates a default LettuceClientConfiguration builder.
     *
     * @param pool the Pool to be used for creating the builder
     * @return a LettuceClientConfigurationBuilder based on the provided Pool
     */
    private LettuceClientConfigurationBuilder createBuilder(Pool pool) {
		if (isPoolEnabled(pool)) {
			return new PoolBuilderFactory().createBuilder(pool);
		}
		return LettuceClientConfiguration.builder();
	}

	/**
     * Applies the properties to the LettuceClientConfigurationBuilder.
     * 
     * @param builder the LettuceClientConfigurationBuilder to apply the properties to
     */
    private void applyProperties(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		if (isSslEnabled()) {
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
	}

	/**
     * Creates the client options for the Lettuce connection.
     * 
     * @return the client options
     */
    private ClientOptions createClientOptions() {
		ClientOptions.Builder builder = initializeClientOptionsBuilder();
		Duration connectTimeout = getProperties().getConnectTimeout();
		if (connectTimeout != null) {
			builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
		}
		if (isSslEnabled() && getProperties().getSsl().getBundle() != null) {
			SslBundle sslBundle = getSslBundles().getBundle(getProperties().getSsl().getBundle());
			io.lettuce.core.SslOptions.Builder sslOptionsBuilder = io.lettuce.core.SslOptions.builder();
			sslOptionsBuilder.keyManager(sslBundle.getManagers().getKeyManagerFactory());
			sslOptionsBuilder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
			SslOptions sslOptions = sslBundle.getOptions();
			if (sslOptions.getCiphers() != null) {
				sslOptionsBuilder.cipherSuites(sslOptions.getCiphers());
			}
			if (sslOptions.getEnabledProtocols() != null) {
				sslOptionsBuilder.protocols(sslOptions.getEnabledProtocols());
			}
			builder.sslOptions(sslOptionsBuilder.build());
		}
		return builder.timeoutOptions(TimeoutOptions.enabled()).build();
	}

	/**
     * Initializes the ClientOptions.Builder based on the properties provided.
     * If the cluster property is not null, it initializes the ClusterClientOptions.Builder
     * and sets the ClusterTopologyRefreshOptions based on the refresh properties.
     * 
     * @return the initialized ClientOptions.Builder
     */
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

	/**
     * Customizes the Lettuce client configuration from a URL.
     * 
     * @param builder the Lettuce client configuration builder
     */
    private void customizeConfigurationFromUrl(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
		if (urlUsesSsl()) {
			builder.useSsl();
		}
	}

	/**
	 * Inner class to allow optional commons-pool2 dependency.
	 */
	private static final class PoolBuilderFactory {

		/**
         * Creates a LettuceClientConfigurationBuilder object with the provided Pool properties.
         * 
         * @param properties the Pool properties to be used for configuring the Lettuce client
         * @return a LettuceClientConfigurationBuilder object with the provided Pool properties
         */
        LettuceClientConfigurationBuilder createBuilder(Pool properties) {
			return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(properties));
		}

		/**
         * Returns a GenericObjectPoolConfig based on the provided Pool properties.
         * 
         * @param properties the Pool properties to configure the GenericObjectPoolConfig
         * @return a GenericObjectPoolConfig configured with the provided properties
         */
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
