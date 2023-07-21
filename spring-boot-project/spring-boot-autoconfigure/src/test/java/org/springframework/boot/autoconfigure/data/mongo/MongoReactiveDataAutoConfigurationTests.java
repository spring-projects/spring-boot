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

package org.springframework.boot.autoconfigure.data.mongo;

import java.time.Duration;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.gridfs.GridFSBucket;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoReactiveDataAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MongoReactiveDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class));

	@Test
	void templateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveMongoTemplate.class));
	}

	@Test
	void whenNoGridFsDatabaseIsConfiguredTheGridFsTemplateUsesTheMainDatabase() {
		this.contextRunner.run((context) -> assertThat(grisFsTemplateDatabaseName(context)).isEqualTo("test"));
	}

	@Test
	void whenGridFsDatabaseIsConfiguredThenGridFsTemplateUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.database:grid")
			.run((context) -> assertThat(grisFsTemplateDatabaseName(context)).isEqualTo("grid"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void usesMongoConnectionDetailsIfAvailable() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsConfiguration.class).run((context) -> {
			assertThat(grisFsTemplateDatabaseName(context)).isEqualTo("grid-database-1");
			ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
			GridFSBucket bucket = ((Mono<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier"))
				.block(Duration.ofSeconds(30));
			assertThat(bucket.getBucketName()).isEqualTo("connection-details-bucket");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void whenGridFsBucketIsConfiguredThenGridFsTemplateUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.bucket:test-bucket").run((context) -> {
			assertThat(context).hasSingleBean(ReactiveGridFsTemplate.class);
			ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
			GridFSBucket bucket = ((Mono<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier"))
				.block(Duration.ofSeconds(30));
			assertThat(bucket.getBucketName()).isEqualTo("test-bucket");
		});
	}

	@Test
	void backsOffIfMongoClientBeanIsNotPresent() {
		ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(PropertyPlaceholderAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class));
		runner.run((context) -> assertThat(context).doesNotHaveBean(MongoReactiveDataAutoConfiguration.class));
	}

	@Test
	void databaseHasDefault() {
		this.contextRunner.run((context) -> {
			ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
			assertThat(factory.getMongoDatabase().block().getName()).isEqualTo("test");
		});
	}

	@Test
	void databasePropertyIsUsed() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.database=mydb").run((context) -> {
			ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
			assertThat(factory.getMongoDatabase().block().getName()).isEqualTo("mydb");
		});
	}

	@Test
	void databaseInUriPropertyIsUsed() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/mydb")
			.run((context) -> {
				ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
				assertThat(factory.getMongoDatabase().block().getName()).isEqualTo("mydb");
			});
	}

	@Test
	void databasePropertyOverridesUriProperty() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/notused",
					"spring.data.mongodb.database=mydb")
			.run((context) -> {
				ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
				assertThat(factory.getMongoDatabase().block().getName()).isEqualTo("mydb");
			});
	}

	@Test
	void databasePropertyIsUsedWhenNoDatabaseInUri() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/",
					"spring.data.mongodb.database=mydb")
			.run((context) -> {
				ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
				assertThat(factory.getMongoDatabase().block().getName()).isEqualTo("mydb");
			});
	}

	@Test
	void contextFailsWhenDatabaseNotSet() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/")
			.run((context) -> assertThat(context).getFailure().hasMessageContaining("Database name must not be empty"));
	}

	@SuppressWarnings("unchecked")
	private String grisFsTemplateDatabaseName(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(ReactiveGridFsTemplate.class);
		ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
		GridFSBucket bucket = ((Mono<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier"))
			.block(Duration.ofSeconds(30));
		MongoCollection<?> collection = (MongoCollection<?>) ReflectionTestUtils.getField(bucket, "filesCollection");
		return collection.getNamespace().getDatabaseName();
	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		MongoConnectionDetails mongoConnectionDetails() {
			return new MongoConnectionDetails() {

				@Override
				public ConnectionString getConnectionString() {
					return new ConnectionString("mongodb://localhost/db");
				}

				@Override
				public GridFs getGridFs() {
					return new GridFs() {

						@Override
						public String getDatabase() {
							return "grid-database-1";
						}

						@Override
						public String getBucket() {
							return "connection-details-bucket";
						}

					};
				}

			};
		}

	}

}
