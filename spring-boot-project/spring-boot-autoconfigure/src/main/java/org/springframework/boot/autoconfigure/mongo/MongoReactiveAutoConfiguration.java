/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.connection.TransportSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactive Mongo.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ MongoClient.class, Flux.class })
@EnableConfigurationProperties(MongoProperties.class)
public class MongoReactiveAutoConfiguration {

	/**
     * Creates a new instance of {@link PropertiesMongoConnectionDetails} if there is no existing bean of type {@link MongoConnectionDetails}.
     * Uses the provided {@link MongoProperties} to construct the {@link PropertiesMongoConnectionDetails}.
     * 
     * @param properties the {@link MongoProperties} used to construct the {@link PropertiesMongoConnectionDetails}
     * @return a new instance of {@link PropertiesMongoConnectionDetails} if there is no existing bean of type {@link MongoConnectionDetails}
     */
    @Bean
	@ConditionalOnMissingBean(MongoConnectionDetails.class)
	PropertiesMongoConnectionDetails mongoConnectionDetails(MongoProperties properties) {
		return new PropertiesMongoConnectionDetails(properties);
	}

	/**
     * Creates a reactive streams MongoDB client bean if no other bean of the same type is present.
     * Uses the provided builder customizers and settings to configure the client.
     *
     * @param builderCustomizers The customizers to apply to the MongoClientSettings builder.
     * @param settings The settings to use for creating the client.
     * @return The created reactive streams MongoDB client.
     */
    @Bean
	@ConditionalOnMissingBean
	public MongoClient reactiveStreamsMongoClient(
			ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers, MongoClientSettings settings) {
		ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(
				builderCustomizers.orderedStream().toList());
		return factory.createMongoClient(settings);
	}

	/**
     * MongoClientSettingsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(MongoClientSettings.class)
	static class MongoClientSettingsConfiguration {

		/**
         * Returns the MongoClientSettings object with default settings.
         * 
         * @return the MongoClientSettings object with default settings
         */
        @Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().build();
		}

		/**
         * Returns a customizer for the standard MongoDB client settings builder.
         * 
         * @param properties the MongoDB properties
         * @param connectionDetails the MongoDB connection details
         * @param sslBundles the SSL bundles (optional)
         * @return the customizer for the standard MongoDB client settings builder
         */
        @Bean
		StandardMongoClientSettingsBuilderCustomizer standardMongoSettingsCustomizer(MongoProperties properties,
				MongoConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
			return new StandardMongoClientSettingsBuilderCustomizer(connectionDetails.getConnectionString(),
					properties.getUuidRepresentation(), properties.getSsl(), sslBundles.getIfAvailable());
		}

	}

	/**
     * NettyDriverConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ SocketChannel.class, NioEventLoopGroup.class })
	static class NettyDriverConfiguration {

		/**
         * Returns a NettyDriverMongoClientSettingsBuilderCustomizer bean with the highest precedence.
         * This bean is responsible for customizing the Netty driver settings for the MongoDB client.
         * 
         * @param settings the provider for MongoClientSettings
         * @return a NettyDriverMongoClientSettingsBuilderCustomizer bean
         */
        @Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		NettyDriverMongoClientSettingsBuilderCustomizer nettyDriverCustomizer(
				ObjectProvider<MongoClientSettings> settings) {
			return new NettyDriverMongoClientSettingsBuilderCustomizer(settings);
		}

	}

	/**
	 * {@link MongoClientSettingsBuilderCustomizer} to apply Mongo client settings.
	 */
	static final class NettyDriverMongoClientSettingsBuilderCustomizer
			implements MongoClientSettingsBuilderCustomizer, DisposableBean {

		private final ObjectProvider<MongoClientSettings> settings;

		private volatile EventLoopGroup eventLoopGroup;

		/**
         * Constructs a new NettyDriverMongoClientSettingsBuilderCustomizer with the specified settings.
         *
         * @param settings the provider of MongoClientSettings
         */
        NettyDriverMongoClientSettingsBuilderCustomizer(ObjectProvider<MongoClientSettings> settings) {
			this.settings = settings;
		}

		/**
         * Customizes the NettyDriverMongoClientSettingsBuilder by setting the transport configuration.
         * If a custom transport configuration is not available, it sets the default NioEventLoopGroup.
         * 
         * @param builder the NettyDriverMongoClientSettingsBuilder to be customized
         */
        @Override
		public void customize(Builder builder) {
			if (!isCustomTransportConfiguration(this.settings.getIfAvailable())) {
				NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
				this.eventLoopGroup = eventLoopGroup;
				builder.transportSettings(TransportSettings.nettyBuilder().eventLoopGroup(eventLoopGroup).build());
			}
		}

		/**
         * This method is called when the object is being destroyed.
         * It shuts down the event loop group gracefully and awaits for the termination.
         * 
         * @throws InterruptedException if the thread is interrupted while awaiting the termination
         */
        @Override
		public void destroy() {
			EventLoopGroup eventLoopGroup = this.eventLoopGroup;
			if (eventLoopGroup != null) {
				eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
				this.eventLoopGroup = null;
			}
		}

		/**
         * Checks if the provided MongoClientSettings object has a custom transport configuration.
         * 
         * @param settings the MongoClientSettings object to check
         * @return true if the settings object has a custom transport configuration, false otherwise
         * @deprecated This method is deprecated and will be removed in a future release.
         */
        @SuppressWarnings("deprecation")
		private boolean isCustomTransportConfiguration(MongoClientSettings settings) {
			return settings != null
					&& (settings.getTransportSettings() != null || settings.getStreamFactoryFactory() != null);
		}

	}

}
