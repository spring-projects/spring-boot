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

package org.springframework.boot.autoconfigure.mongo.embedded;

import java.net.UnknownHostException;

import com.mongodb.CommandResult;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.distribution.Feature;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.SocketUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link EmbeddedMongoAutoConfiguration}.
 *
 * @author Henryk Konsek
 * @author Andy Wilkinson
 */
public class EmbeddedMongoAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultVersion() {
		assertVersionConfiguration(null, "2.6.10");
	}

	@Test
	public void customVersion() {
		assertVersionConfiguration("2.7.1", "2.7.1");
	}

	@Test
	public void customFeatures() {
		this.context = new AnnotationConfigApplicationContext();
		int mongoPort = SocketUtils.findAvailableTcpPort();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.port=" + mongoPort,
				"spring.mongodb.embedded.features=TEXT_SEARCH, SYNC_DELAY");
		this.context.register(EmbeddedMongoAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(EmbeddedMongoProperties.class).getFeatures(),
				hasItems(Feature.TEXT_SEARCH, Feature.SYNC_DELAY));
	}

	@Test
	public void randomlyAllocatedPortIsAvailableWhenCreatingMongoClient() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.port=0");
		this.context.register(EmbeddedMongoAutoConfiguration.class,
				MongoClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(MongoClient.class).getAddress().getPort(),
				equalTo(Integer.valueOf(
						this.context.getEnvironment().getProperty("local.mongo.port"))));
	}

	@Test
	public void portIsAvailableInParentContext() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();
		try {
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.data.mongodb.port=0");
			this.context.register(EmbeddedMongoAutoConfiguration.class,
					MongoClientConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			this.context.refresh();
			assertThat(parent.getEnvironment().getProperty("local.mongo.port"),
					is(notNullValue()));
		}
		finally {
			parent.close();
		}
	}

	private void assertVersionConfiguration(String configuredVersion,
			String expectedVersion) {
		this.context = new AnnotationConfigApplicationContext();
		int mongoPort = SocketUtils.findAvailableTcpPort();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.port=" + mongoPort);
		if (configuredVersion != null) {
			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.mongodb.embedded.version=" + configuredVersion);
		}
		this.context.register(MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class);
		this.context.refresh();
		MongoTemplate mongo = this.context.getBean(MongoTemplate.class);
		CommandResult buildInfo = mongo.executeCommand("{ buildInfo: 1 }");

		assertThat(buildInfo.getString("version"), equalTo(expectedVersion));
	}

	@Configuration
	static class MongoClientConfiguration {

		@Bean
		public MongoClient mongoClient(@Value("${local.mongo.port}") int port)
				throws UnknownHostException {
			return new MongoClient("localhost", port);
		}

	}

}
