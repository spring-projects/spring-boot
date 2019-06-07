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

import java.util.List;

import javax.annotation.PreDestroy;

import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import io.netty.channel.socket.SocketChannel;

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
@Configuration
@ConditionalOnClass(MongoClient.class)
@EnableConfigurationProperties(MongoProperties.class)
public class MongoReactiveAutoConfiguration {

	private final MongoClientSettings settings;

	private MongoClient mongo;

	public MongoReactiveAutoConfiguration(ObjectProvider<MongoClientSettings> settings) {
		this.settings = settings.getIfAvailable();
	}

	@PreDestroy
	public void close() {
		if (this.mongo != null) {
			this.mongo.close();
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoClient reactiveStreamsMongoClient(MongoProperties properties, Environment environment,
			ObjectProvider<List<MongoClientSettingsBuilderCustomizer>> builderCustomizers) {
		ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(properties, environment,
				builderCustomizers.getIfAvailable());
		this.mongo = factory.createMongoClient(this.settings);
		return this.mongo;
	}

	@Configuration
	@ConditionalOnClass(SocketChannel.class)
	static class NettyDriverConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public MongoClientSettingsBuilderCustomizer nettyDriverCustomizer(
				ObjectProvider<MongoClientSettings> settings) {
			return (builder) -> {
				if (!isStreamFactoryFactoryDefined(settings.getIfAvailable())) {
					builder.streamFactoryFactory(NettyStreamFactoryFactory.builder().build());
				}
			};
		}

		private boolean isStreamFactoryFactoryDefined(MongoClientSettings settings) {
			return settings != null && settings.getStreamFactoryFactory() != null;
		}

	}

}
