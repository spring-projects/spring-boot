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

package org.springframework.boot.autoconfigure.mongo;

import java.util.concurrent.TimeUnit;

import com.mongodb.ReadPreference;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.StreamFactory;
import com.mongodb.connection.StreamFactoryFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoReactiveAutoConfiguration}.
 *
 * @author Mark Paluch
 */
public class MongoReactiveAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void clientExists() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(MongoClient.class).length)
				.isEqualTo(1);
	}

	@Test
	public void optionsAdded() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.host:localhost").applyTo(this.context);
		this.context.register(OptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(MongoClient.class).getSettings()
				.getSocketSettings().getReadTimeout(TimeUnit.SECONDS)).isEqualTo(300);
	}

	@Test
	public void optionsAddedButNoHost() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.uri:mongodb://localhost/test")
				.applyTo(this.context);
		this.context.register(OptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class);
		this.context.refresh();
		assertThat(
				this.context.getBean(MongoClient.class).getSettings().getReadPreference())
						.isEqualTo(ReadPreference.nearest());
	}

	@Test
	public void optionsSslConfig() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.uri:mongodb://localhost/test")
				.applyTo(this.context);
		this.context.register(SslOptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class);
		this.context.refresh();
		MongoClient mongo = this.context.getBean(MongoClient.class);
		MongoClientSettings settings = mongo.getSettings();
		assertThat(settings.getApplicationName()).isEqualTo("test-config");
		assertThat(settings.getStreamFactoryFactory())
				.isSameAs(this.context.getBean("myStreamFactoryFactory"));
	}

	@Test
	public void customizerOverridesAutoConfig() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.data.mongodb.uri:mongodb://localhost/test?appname=auto-config")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class, SimpleCustomizerConfig.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(MongoClient.class).length)
				.isEqualTo(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		assertThat(client.getSettings().getApplicationName())
				.isEqualTo("overridden-name");
	}

	@Configuration
	static class OptionsConfig {

		@Bean
		public MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().readPreference(ReadPreference.nearest())
					.socketSettings(SocketSettings.builder()
							.readTimeout(300, TimeUnit.SECONDS).build())
					.build();
		}

	}

	@Configuration
	static class SslOptionsConfig {

		@Bean
		public MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().applicationName("test-config")
					.streamFactoryFactory(myStreamFactoryFactory()).build();
		}

		@Bean
		public StreamFactoryFactory myStreamFactoryFactory() {
			StreamFactoryFactory streamFactoryFactory = mock(StreamFactoryFactory.class);
			given(streamFactoryFactory.create(any(), any()))
					.willReturn(mock(StreamFactory.class));
			return streamFactoryFactory;
		}

	}

	@Configuration
	static class SimpleCustomizerConfig {

		@Bean
		public MongoClientSettingsBuilderCustomizer customizer() {
			return (clientSettingsBuilder) -> clientSettingsBuilder
					.applicationName("overridden-name");
		}

	}

}
