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

package org.springframework.boot.autoconfigure.mongo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.connection.AsynchronousSocketChannelStreamFactoryFactory;
import com.mongodb.connection.StreamFactory;
import com.mongodb.connection.StreamFactoryFactory;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.internal.MongoClientImpl;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoReactiveAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class MongoReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoReactiveAutoConfiguration.class));

	@Test
	void clientExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoClient.class));
	}

	@Test
	void settingsAdded() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.host:localhost")
				.withUserConfiguration(SettingsConfig.class)
				.run((context) -> assertThat(getSettings(context).getSocketSettings().getReadTimeout(TimeUnit.SECONDS))
						.isEqualTo(300));
	}

	@Test
	void settingsAddedButNoHost() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test")
				.withUserConfiguration(SettingsConfig.class)
				.run((context) -> assertThat(getSettings(context).getReadPreference())
						.isEqualTo(ReadPreference.nearest()));
	}

	@Test
	void settingsSslConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test")
				.withUserConfiguration(SslSettingsConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(MongoClient.class);
					MongoClientSettings settings = getSettings(context);
					assertThat(settings.getApplicationName()).isEqualTo("test-config");
					assertThat(settings.getStreamFactoryFactory()).isSameAs(context.getBean("myStreamFactoryFactory"));
				});
	}

	@Test
	void nettyStreamFactoryFactoryIsConfiguredAutomatically() {
		AtomicReference<EventLoopGroup> eventLoopGroupReference = new AtomicReference<>();
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MongoClient.class);
			StreamFactoryFactory factory = getSettings(context).getStreamFactoryFactory();
			assertThat(factory).isInstanceOf(NettyStreamFactoryFactory.class);
			EventLoopGroup eventLoopGroup = (EventLoopGroup) ReflectionTestUtils.getField(factory, "eventLoopGroup");
			assertThat(eventLoopGroup.isShutdown()).isFalse();
			eventLoopGroupReference.set(eventLoopGroup);
		});
		assertThat(eventLoopGroupReference.get().isShutdown()).isTrue();
	}

	@Test
	void customizerOverridesAutoConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test?appname=auto-config")
				.withUserConfiguration(SimpleCustomizerConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(MongoClient.class);
					MongoClientSettings settings = getSettings(context);
					assertThat(settings.getApplicationName()).isEqualTo("overridden-name");
					assertThat(settings.getStreamFactoryFactory())
							.isEqualTo(SimpleCustomizerConfig.streamFactoryFactory);
				});
	}

	private MongoClientSettings getSettings(ApplicationContext context) {
		MongoClientImpl client = (MongoClientImpl) context.getBean(MongoClient.class);
		return client.getSettings();
	}

	@Configuration(proxyBeanMethods = false)
	static class SettingsConfig {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().readPreference(ReadPreference.nearest())
					.applyToSocketSettings((socket) -> socket.readTimeout(300, TimeUnit.SECONDS)).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SslSettingsConfig {

		@Bean
		MongoClientSettings mongoClientSettings(StreamFactoryFactory streamFactoryFactory) {
			return MongoClientSettings.builder().applicationName("test-config")
					.streamFactoryFactory(streamFactoryFactory).build();
		}

		@Bean
		StreamFactoryFactory myStreamFactoryFactory() {
			StreamFactoryFactory streamFactoryFactory = mock(StreamFactoryFactory.class);
			given(streamFactoryFactory.create(any(), any())).willReturn(mock(StreamFactory.class));
			return streamFactoryFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleCustomizerConfig {

		private static final StreamFactoryFactory streamFactoryFactory = new AsynchronousSocketChannelStreamFactoryFactory.Builder()
				.build();

		@Bean
		MongoClientSettingsBuilderCustomizer customizer() {
			return (clientSettingsBuilder) -> clientSettingsBuilder.applicationName("overridden-name")
					.streamFactoryFactory(streamFactoryFactory);
		}

	}

}
