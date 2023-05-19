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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.connection.AsynchronousSocketChannelStreamFactoryFactory;
import com.mongodb.connection.SslSettings;
import com.mongodb.connection.StreamFactory;
import com.mongodb.connection.StreamFactoryFactory;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.internal.MongoClientImpl;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
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
		.withConfiguration(AutoConfigurations.of(MongoReactiveAutoConfiguration.class, SslAutoConfiguration.class));

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
			.run((context) -> assertThat(getSettings(context).getReadPreference()).isEqualTo(ReadPreference.nearest()));
	}

	@Test
	void settingsSslConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test")
			.withUserConfiguration(SslSettingsConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(MongoClient.class);
				MongoClientSettings settings = getSettings(context);
				assertThat(settings.getApplicationName()).isEqualTo("test-config");
				assertThat(settings.getStreamFactoryFactory()).isSameAs(context.getBean("myStreamFactoryFactory"));
			});
	}

	@Test
	void configuresSslWhenEnabled() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.ssl.enabled=true").run((context) -> {
			SslSettings sslSettings = getSettings(context).getSslSettings();
			assertThat(sslSettings.isEnabled()).isTrue();
			assertThat(sslSettings.getContext()).isNull();
		});
	}

	@Test
	void configuresSslWithBundle() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.ssl.bundle=test-bundle",
					"spring.ssl.bundle.jks.test-bundle.keystore.location=classpath:test.jks",
					"spring.ssl.bundle.jks.test-bundle.keystore.password=secret",
					"spring.ssl.bundle.jks.test-bundle.key.password=password")
			.run((context) -> {
				SslSettings sslSettings = getSettings(context).getSslSettings();
				assertThat(sslSettings.isEnabled()).isTrue();
				assertThat(sslSettings.getContext()).isNotNull();
			});
	}

	@Test
	void configuresWithoutSslWhenDisabledWithBundle() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.ssl.enabled=false", "spring.data.mongodb.ssl.bundle=test-bundle")
			.run((context) -> {
				SslSettings sslSettings = getSettings(context).getSslSettings();
				assertThat(sslSettings.isEnabled()).isFalse();
			});
	}

	@Test
	void doesNotConfigureCredentialsWithoutUsername() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.password=secret",
					"spring.data.mongodb.authentication-database=authdb")
			.run((context) -> assertThat(getSettings(context).getCredential()).isNull());
	}

	@Test
	void configuresCredentialsFromPropertiesWithDefaultDatabase() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.username=user", "spring.data.mongodb.password=secret")
			.run((context) -> {
				MongoCredential credential = getSettings(context).getCredential();
				assertThat(credential.getUserName()).isEqualTo("user");
				assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
				assertThat(credential.getSource()).isEqualTo("test");
			});
	}

	@Test
	void configuresCredentialsFromPropertiesWithDatabase() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.username=user", "spring.data.mongodb.password=secret",
					"spring.data.mongodb.database=mydb")
			.run((context) -> {
				MongoCredential credential = getSettings(context).getCredential();
				assertThat(credential.getUserName()).isEqualTo("user");
				assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
				assertThat(credential.getSource()).isEqualTo("mydb");
			});
	}

	@Test
	void configuresCredentialsFromPropertiesWithAuthDatabase() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.username=user", "spring.data.mongodb.password=secret",
					"spring.data.mongodb.database=mydb", "spring.data.mongodb.authentication-database=authdb")
			.run((context) -> {
				MongoCredential credential = getSettings(context).getCredential();
				assertThat(credential.getUserName()).isEqualTo("user");
				assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
				assertThat(credential.getSource()).isEqualTo("authdb");
			});
	}

	@Test
	void doesNotConfigureCredentialsWithoutUsernameInUri() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=mongodb://localhost/mydb?authSource=authdb")
			.run((context) -> assertThat(getSettings(context).getCredential()).isNull());
	}

	@Test
	void configuresCredentialsFromUriPropertyWithDefaultDatabase() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=mongodb://user:secret@localhost/")
			.run((context) -> {
				MongoCredential credential = getSettings(context).getCredential();
				assertThat(credential.getUserName()).isEqualTo("user");
				assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
				assertThat(credential.getSource()).isEqualTo("admin");
			});
	}

	@Test
	void configuresCredentialsFromUriPropertyWithDatabase() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.uri=mongodb://user:secret@localhost/mydb",
					"spring.data.mongodb.database=notused", "spring.data.mongodb.authentication-database=notused")
			.run((context) -> {
				MongoCredential credential = getSettings(context).getCredential();
				assertThat(credential.getUserName()).isEqualTo("user");
				assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
				assertThat(credential.getSource()).isEqualTo("mydb");
			});
	}

	@Test
	void configuresCredentialsFromUriPropertyWithAuthDatabase() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.uri=mongodb://user:secret@localhost/mydb?authSource=authdb",
					"spring.data.mongodb.database=notused", "spring.data.mongodb.authentication-database=notused")
			.run((context) -> {
				MongoCredential credential = getSettings(context).getCredential();
				assertThat(credential.getUserName()).isEqualTo("user");
				assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
				assertThat(credential.getSource()).isEqualTo("authdb");
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
			.withUserConfiguration(SimpleCustomizerConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(MongoClient.class);
				MongoClientSettings settings = getSettings(context);
				assertThat(settings.getApplicationName()).isEqualTo("overridden-name");
				assertThat(settings.getStreamFactoryFactory()).isEqualTo(SimpleCustomizerConfig.streamFactoryFactory);
			});
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(PropertiesMongoConnectionDetails.class));
	}

	@Test
	void shouldUseCustomConnectionDetailsWhenDefined() {
		this.contextRunner.withBean(MongoConnectionDetails.class, () -> new MongoConnectionDetails() {

			@Override
			public ConnectionString getConnectionString() {
				return new ConnectionString("mongodb://localhost");
			}

		})
			.run((context) -> assertThat(context).hasSingleBean(MongoConnectionDetails.class)
				.doesNotHaveBean(PropertiesMongoConnectionDetails.class));
	}

	private MongoClientSettings getSettings(ApplicationContext context) {
		MongoClientImpl client = (MongoClientImpl) context.getBean(MongoClient.class);
		return client.getSettings();
	}

	@Configuration(proxyBeanMethods = false)
	static class SettingsConfig {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder()
				.readPreference(ReadPreference.nearest())
				.applyToSocketSettings((socket) -> socket.readTimeout(300, TimeUnit.SECONDS))
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SslSettingsConfig {

		@Bean
		MongoClientSettings mongoClientSettings(StreamFactoryFactory streamFactoryFactory) {
			return MongoClientSettings.builder()
				.applicationName("test-config")
				.streamFactoryFactory(streamFactoryFactory)
				.build();
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
