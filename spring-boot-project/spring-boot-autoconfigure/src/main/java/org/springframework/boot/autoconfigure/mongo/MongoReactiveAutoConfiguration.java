/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.stream.Collectors;

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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactive Mongo.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ MongoClient.class, Flux.class })
@EnableConfigurationProperties(MongoProperties.class)
public class MongoReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MongoClient reactiveStreamsMongoClient(MongoProperties properties, Environment environment,
			ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
			ObjectProvider<MongoClientSettings> settings) {
		ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(properties, environment,
				builderCustomizers.orderedStream().collect(Collectors.toList()));
		return factory.createMongoClient(settings.getIfAvailable());
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
	private static final class NettyDriverMongoClientSettingsBuilderCustomizer
			implements MongoClientSettingsBuilderCustomizer, DisposableBean {

		private final ObjectProvider<MongoClientSettings> settings;

		private volatile EventLoopGroup eventLoopGroup;

		private NettyDriverMongoClientSettingsBuilderCustomizer(ObjectProvider<MongoClientSettings> settings) {
			this.settings = settings;
		}

		@Override
		public void customize(Builder builder) {
			if (!isStreamFactoryFactoryDefined(this.settings.getIfAvailable())) {
				NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
				this.eventLoopGroup = eventLoopGroup;
				builder.streamFactoryFactory(
						NettyStreamFactoryFactory.builder().eventLoopGroup(eventLoopGroup).build());
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
