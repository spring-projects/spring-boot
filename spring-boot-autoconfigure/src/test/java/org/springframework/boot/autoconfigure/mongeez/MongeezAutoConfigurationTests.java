/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongeez;

import com.mongodb.MongoClientOptions;
import org.junit.After;
import org.junit.Test;
import org.mongeez.MongeezRunner;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Eddú Meléndez
 * @since 1.3.0
 */
public class MongeezAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void setMongeezChangelog() {
		this.context = doLoad(MongoConfiguration.class, "mongeez.dbName=mongeezTest");
		assertThat(this.context.getBeanNamesForType(MongeezRunner.class).length, is(1));
	}

	private AnnotationConfigApplicationContext doLoad(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext =
			new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(MongoAutoConfiguration.class, MongeezAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		applicationContext.register(config);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class MongoConfiguration {

		@Bean
		public MongoClientOptions mongoOptions() {
			return MongoClientOptions.builder().socketTimeout(300).build();
		}

	}

}
