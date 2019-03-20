/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.net.SocketFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class MongoAutoConfigurationTests {

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
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(MongoClient.class)).hasSize(1);
	}

	@Test
	public void optionsAdded() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.host:localhost");
		this.context.register(OptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(MongoClient.class).getMongoClientOptions()
				.getSocketTimeout()).isEqualTo(300);
	}

	@Test
	public void optionsAddedButNoHost() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.uri:mongodb://localhost/test");
		this.context.register(OptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(MongoClient.class).getMongoClientOptions()
				.getSocketTimeout()).isEqualTo(300);
	}

	@Test
	public void optionsSslConfig() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.uri:mongodb://localhost/test");
		this.context.register(SslOptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class);
		this.context.refresh();
		MongoClient mongo = this.context.getBean(MongoClient.class);
		MongoClientOptions options = mongo.getMongoClientOptions();
		assertThat(options.isSslEnabled()).isTrue();
		assertThat(options.getSocketFactory())
				.isSameAs(this.context.getBean("mySocketFactory"));
	}

	@Configuration
	static class OptionsConfig {

		@Bean
		public MongoClientOptions mongoOptions() {
			return MongoClientOptions.builder().socketTimeout(300).build();
		}

	}

	@Configuration
	static class SslOptionsConfig {

		@Bean
		public MongoClientOptions mongoClientOptions() {
			return MongoClientOptions.builder().sslEnabled(true)
					.socketFactory(mySocketFactory()).build();
		}

		@Bean
		public SocketFactory mySocketFactory() {
			return mock(SocketFactory.class);
		}

	}

}
