/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.mongodb.autoconfigure;

import java.time.Duration;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.gridfs.GridFSBucket;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataMongoReactiveAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class DataMongoReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
				MongoReactiveAutoConfiguration.class, DataMongoReactiveAutoConfiguration.class));

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
	void whenGridFsBucketIsConfiguredThenGridFsTemplateUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.bucket:test-bucket").run((context) -> {
			assertThat(context).hasSingleBean(ReactiveGridFsTemplate.class);
			ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
			Mono<GridFSBucket> field = (Mono<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier");
			assertThat(field).isNotNull();
			GridFSBucket bucket = field.block(Duration.ofSeconds(30));
			assertThat(bucket).isNotNull();
			assertThat(bucket.getBucketName()).isEqualTo("test-bucket");
		});
	}

	@Test
	void backsOffIfMongoClientBeanIsNotPresent() {
		ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(PropertyPlaceholderAutoConfiguration.class, DataMongoReactiveAutoConfiguration.class));
		runner.run((context) -> assertThat(context).doesNotHaveBean(DataMongoReactiveAutoConfiguration.class));
	}

	@Test
	void databaseHasDefault() {
		this.contextRunner.run((context) -> {
			ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
			MongoDatabase mongoDatabase = factory.getMongoDatabase().block();
			assertThat(mongoDatabase).isNotNull();
			assertThat(mongoDatabase.getName()).isEqualTo("test");
		});
	}

	@Test
	void databasePropertyIsUsed() {
		this.contextRunner.withPropertyValues("spring.mongodb.database=mydb").run((context) -> {
			ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
			MongoDatabase mongoDatabase = factory.getMongoDatabase().block();
			assertThat(mongoDatabase).isNotNull();
			assertThat(mongoDatabase.getName()).isEqualTo("mydb");
		});
	}

	@Test
	void databaseInUriPropertyIsUsed() {
		this.contextRunner.withPropertyValues("spring.mongodb.uri=mongodb://mongo.example.com/mydb").run((context) -> {
			ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
			MongoDatabase mongoDatabase = factory.getMongoDatabase().block();
			assertThat(mongoDatabase).isNotNull();
			assertThat(mongoDatabase.getName()).isEqualTo("mydb");
		});
	}

	@Test
	void databasePropertyOverridesUriProperty() {
		this.contextRunner
			.withPropertyValues("spring.mongodb.uri=mongodb://mongo.example.com/notused",
					"spring.mongodb.database=mydb")
			.run((context) -> {
				ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
				MongoDatabase mongoDatabase = factory.getMongoDatabase().block();
				assertThat(mongoDatabase).isNotNull();
				assertThat(mongoDatabase.getName()).isEqualTo("mydb");
			});
	}

	@Test
	void databasePropertyIsUsedWhenNoDatabaseInUri() {
		this.contextRunner
			.withPropertyValues("spring.mongodb.uri=mongodb://mongo.example.com/", "spring.mongodb.database=mydb")
			.run((context) -> {
				ReactiveMongoDatabaseFactory factory = context.getBean(ReactiveMongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleReactiveMongoDatabaseFactory.class);
				MongoDatabase mongoDatabase = factory.getMongoDatabase().block();
				assertThat(mongoDatabase).isNotNull();
				assertThat(mongoDatabase.getName()).isEqualTo("mydb");
			});
	}

	@Test
	void contextFailsWhenDatabaseNotSet() {
		this.contextRunner.withPropertyValues("spring.mongodb.uri=mongodb://mongo.example.com/")
			.run((context) -> assertThat(context).getFailure().hasMessageContaining("Database name must not be empty"));
	}

	@Test
	void mappingMongoConverterHasANoOpDbRefResolver() {
		this.contextRunner.run((context) -> {
			MappingMongoConverter converter = context.getBean(MappingMongoConverter.class);
			assertThat(converter).extracting("dbRefResolver").isInstanceOf(NoOpDbRefResolver.class);
		});
	}

	@SuppressWarnings("unchecked")
	private String grisFsTemplateDatabaseName(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(ReactiveGridFsTemplate.class);
		ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
		Mono<GridFSBucket> field = (Mono<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier");
		assertThat(field).isNotNull();
		GridFSBucket bucket = field.block(Duration.ofSeconds(30));
		assertThat(bucket).isNotNull();
		MongoCollection<?> collection = (MongoCollection<?>) ReflectionTestUtils.getField(bucket, "filesCollection");
		assertThat(collection).isNotNull();
		return collection.getNamespace().getDatabaseName();
	}

}
