/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.mongo;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.mongodb.BasicDBList;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ServerDescription;
import de.flapdoodle.embed.mongo.config.IMongoCmdOptions;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.awaitility.Awaitility;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using {@link DataMongoTest @DataMongoTest} with transactions.
 *
 * @author Andy Wilkinson
 */
@DataMongoTest
@Transactional
class TransactionalDataMongoTestIntegrationTests {

	@Autowired
	private ExampleRepository exampleRepository;

	@Test
	void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument);
		assertThat(exampleDocument.getId()).isNotNull();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TransactionManagerConfiguration {

		@Bean
		MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
			return new MongoTransactionManager(dbFactory);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class MongoCustomizationConfiguration {

		private static final String REPLICA_SET_NAME = "rs1";

		@Bean
		IMongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {
			IMongoCmdOptions cmdOptions = new MongoCmdOptionsBuilder().useNoJournal(false).build();
			return new MongodConfigBuilder().version(Version.Main.PRODUCTION)
					.replication(new Storage(null, REPLICA_SET_NAME, 0)).cmdOptions(cmdOptions)
					.stopTimeoutInMillis(60000).build();
		}

		@Bean
		MongoInitializer mongoInitializer(MongoClient client, MongoTemplate template) {
			return new MongoInitializer(client, template);
		}

		static class MongoInitializer implements InitializingBean {

			private final MongoClient client;

			private final MongoTemplate template;

			MongoInitializer(MongoClient client, MongoTemplate template) {
				this.client = client;
				this.template = template;
			}

			@Override
			public void afterPropertiesSet() throws Exception {
				List<ServerDescription> servers = this.client.getClusterDescription().getServerDescriptions();
				assertThat(servers).hasSize(1);
				ServerAddress address = servers.get(0).getAddress();
				BasicDBList members = new BasicDBList();
				members.add(new Document("_id", 0).append("host", address.getHost() + ":" + address.getPort()));
				Document config = new Document("_id", REPLICA_SET_NAME);
				config.put("members", members);
				MongoDatabase admin = this.client.getDatabase("admin");
				admin.runCommand(new Document("replSetInitiate", config));
				Awaitility.await().atMost(Duration.ofMinutes(1)).until(() -> {
					try (ClientSession session = this.client.startSession()) {
						return true;
					}
					catch (Exception ex) {
						return false;
					}
				});
				this.template.createCollection("exampleDocuments");
			}

		}

	}

}
