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
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
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

	@Bean
	@ConditionalOnMissingBean(MongoConnectionDetails.class)
	PropertiesMongoConnectionDetails mongoConnectionDetails(MongoProperties properties) {
		return new PropertiesMongoConnectionDetails(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoClient reactiveStreamsMongoClient(
			ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers, MongoClientSettings settings) {
		ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(
				builderCustomizers.orderedStream().toList());
		return factory.createMongoClient(settings);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(MongoClientSettings.class)
	static class MongoClientSettingsConfiguration {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().build();
		}

		@Bean
		StandardMongoClientSettingsBuilderCustomizer standardMongoSettingsCustomizer(MongoProperties properties,
				MongoConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
			return new StandardMongoClientSettingsBuilderCustomizer(connectionDetails.getConnectionString(),
					properties.getUuidRepresentation(), properties.getSsl(), sslBundles.getIfAvailable());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ SocketChannel.class, NioEventLoopGroup.class })
	static class NettyDriverConfiguration {

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

		NettyDriverMongoClientSettingsBuilderCustomizer(ObjectProvider<MongoClientSettings> settings) {
			this.settings = settings;
		}

		@Override
		public void customize(Builder builder) {
			if (!isStreamFactoryFactoryDefined(this.settings.getIfAvailable())) {
				NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
				this.eventLoopGroup = eventLoopGroup;
				builder
					.streamFactoryFactory(NettyStreamFactoryFactory.builder().eventLoopGroup(eventLoopGroup).build());
			}
		}

		@Override
		public void destroy() {
			EventLoopGroup eventLoopGroup = this.eventLoopGroup;
			if (eventLoopGroup != null) {
				eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
				this.eventLoopGroup = null;
			}
		}

		private boolean isStreamFactoryFactoryDefined(MongoClientSettings settings) {
			return settings != null && settings.getStreamFactoryFactory() != null;
		}

	}

}
