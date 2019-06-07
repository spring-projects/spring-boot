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

package org.springframework.boot.autoconfigure.mongo.embedded;

import java.io.File;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Feature;
import org.bson.Document;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedMongoAutoConfiguration}.
 *
 * @author Henryk Konsek
 * @author Andy Wilkinson
 * @author Stephane Nicoll
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
		assertVersionConfiguration(null, "3.2.2");
	}

	@Test
	public void customVersion() {
		assertVersionConfiguration("2.7.1", "2.7.1");
	}

	@Test
	public void customFeatures() {
		load("spring.mongodb.embedded.features=TEXT_SEARCH, SYNC_DELAY");
		assertThat(this.context.getBean(EmbeddedMongoProperties.class).getFeatures()).contains(Feature.TEXT_SEARCH,
				Feature.SYNC_DELAY);
	}

	@Test
	public void useRandomPortByDefault() {
		load();
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(client.getAddress().getPort()).isEqualTo(mongoPort);
	}

	@Test
	public void specifyPortToZeroAllocateRandomPort() {
		load("spring.data.mongodb.port=0");
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(client.getAddress().getPort()).isEqualTo(mongoPort);
	}

	@Test
	public void randomlyAllocatedPortIsAvailableWhenCreatingMongoClient() {
		load(MongoClientConfiguration.class);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(client.getAddress().getPort()).isEqualTo(mongoPort);
	}

	@Test
	public void portIsAvailableInParentContext() {
		try (ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext()) {
			parent.refresh();
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			this.context.register(EmbeddedMongoAutoConfiguration.class, MongoClientConfiguration.class);
			this.context.refresh();
			assertThat(parent.getEnvironment().getProperty("local.mongo.port")).isNotNull();
		}
	}

	@Test
	public void defaultStorageConfiguration() {
		load(MongoClientConfiguration.class);
		Storage replication = this.context.getBean(IMongodConfig.class).replication();
		assertThat(replication.getOplogSize()).isEqualTo(0);
		assertThat(replication.getDatabaseDir()).isNull();
		assertThat(replication.getReplSetName()).isNull();
	}

	@Test
	public void mongoWritesToCustomDatabaseDir() {
		File customDatabaseDir = new File("target/custom-database-dir");
		FileSystemUtils.deleteRecursively(customDatabaseDir);
		load("spring.mongodb.embedded.storage.databaseDir=" + customDatabaseDir.getPath());
		assertThat(customDatabaseDir).isDirectory();
		assertThat(customDatabaseDir.listFiles()).isNotEmpty();
	}

	@Test
	public void customOpLogSizeIsAppliedToConfiguration() {
		load("spring.mongodb.embedded.storage.oplogSize=10");
		assertThat(this.context.getBean(IMongodConfig.class).replication().getOplogSize()).isEqualTo(10);
	}

	@Test
	public void customReplicaSetNameIsAppliedToConfiguration() {
		load("spring.mongodb.embedded.storage.replSetName=testing");
		assertThat(this.context.getBean(IMongodConfig.class).replication().getReplSetName()).isEqualTo("testing");
	}

	private void assertVersionConfiguration(String configuredVersion, String expectedVersion) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.port=0").applyTo(this.context);
		if (configuredVersion != null) {
			TestPropertyValues.of("spring.mongodb.embedded.version=" + configuredVersion).applyTo(this.context);
		}
		this.context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
				EmbeddedMongoAutoConfiguration.class);
		this.context.refresh();
		MongoTemplate mongo = this.context.getBean(MongoTemplate.class);
		Document buildInfo = mongo.executeCommand("{ buildInfo: 1 }");

		assertThat(buildInfo.getString("version")).isEqualTo(expectedVersion);
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class MongoClientConfiguration {

		@Bean
		public MongoClient mongoClient(@Value("${local.mongo.port}") int port) {
			return new MongoClient("localhost", port);
		}

	}

}
