/*
 * Copyright 2012-2014 the original author or authors.
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

import com.mongodb.Mongo;
import com.mongodb.MongoClientOptions;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link MongoAutoConfiguration}.
 *
 * @author Dave Syer
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
		assertEquals(1, this.context.getBeanNamesForType(Mongo.class).length);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void optionsAdded() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.host:localhost");
		this.context.register(OptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class);
		this.context.refresh();
		assertEquals(300,
				this.context.getBean(Mongo.class).getMongoOptions().getSocketTimeout());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void optionsAddedButNoHost() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.uri:mongodb://localhost/test");
		this.context.register(OptionsConfig.class,
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class);
		this.context.refresh();
		assertEquals(300,
				this.context.getBean(Mongo.class).getMongoOptions().getSocketTimeout());
	}

	@Configuration
	protected static class OptionsConfig {

		@Bean
		public MongoClientOptions mongoOptions() {
			return MongoClientOptions.builder().socketTimeout(300).build();
		}

	}

}
