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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Supplier;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.city.City;
import org.springframework.boot.autoconfigure.data.mongo.country.Country;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.PropertiesMongoConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoDataAutoConfiguration}.
 *
 * @author Josh Long
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MongoDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class));

	@Test
	void templateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoTemplate.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void whenGridFsDatabaseIsConfiguredThenGridFsTemplateIsAutoConfiguredAndUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.database:grid").run((context) -> {
			assertThat(context).hasSingleBean(GridFsTemplate.class);
			GridFsTemplate template = context.getBean(GridFsTemplate.class);
			GridFSBucket bucket = ((Supplier<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier"))
				.get();
			assertThat(bucket).extracting("filesCollection", InstanceOfAssertFactories.type(MongoCollection.class))
				.extracting((collection) -> collection.getNamespace().getDatabaseName())
				.isEqualTo("grid");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void usesMongoConnectionDetailsIfAvailable() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(GridFsTemplate.class);
			GridFsTemplate template = context.getBean(GridFsTemplate.class);
			GridFSBucket bucket = ((Supplier<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier"))
				.get();
			assertThat(bucket.getBucketName()).isEqualTo("connection-details-bucket");
			assertThat(bucket).extracting("filesCollection", InstanceOfAssertFactories.type(MongoCollection.class))
				.extracting((collection) -> collection.getNamespace().getDatabaseName())
				.isEqualTo("grid-database-1");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void whenGridFsBucketIsConfiguredThenGridFsTemplateIsAutoConfiguredAndUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.bucket:test-bucket").run((context) -> {
			assertThat(context).hasSingleBean(GridFsTemplate.class);
			GridFsTemplate template = context.getBean(GridFsTemplate.class);
			GridFSBucket bucket = ((Supplier<GridFSBucket>) ReflectionTestUtils.getField(template, "bucketSupplier"))
				.get();
			assertThat(bucket.getBucketName()).isEqualTo("test-bucket");
		});
	}

	@Test
	void customConversions() {
		this.contextRunner.withUserConfiguration(CustomConversionsConfig.class).run((context) -> {
			MongoTemplate template = context.getBean(MongoTemplate.class);
			assertThat(template.getConverter().getConversionService().canConvert(MongoClient.class, Boolean.class))
				.isTrue();
		});
	}

	@Test
	void usesAutoConfigurationPackageToPickUpDocumentTypes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(context, cityPackage);
		context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		try {
			context.refresh();
			assertDomainTypesDiscovered(context.getBean(MongoMappingContext.class), City.class);
		}
		finally {
			context.close();
		}
	}

	@Test
	void defaultFieldNamingStrategy() {
		this.contextRunner.run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			FieldNamingStrategy fieldNamingStrategy = (FieldNamingStrategy) ReflectionTestUtils.getField(mappingContext,
					"fieldNamingStrategy");
			assertThat(fieldNamingStrategy.getClass()).isEqualTo(PropertyNameFieldNamingStrategy.class);
		});
	}

	@Test
	void customFieldNamingStrategy() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.field-naming-strategy:"
					+ CamelCaseAbbreviatingFieldNamingStrategy.class.getName())
			.run((context) -> {
				MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
				FieldNamingStrategy fieldNamingStrategy = (FieldNamingStrategy) ReflectionTestUtils
					.getField(mappingContext, "fieldNamingStrategy");
				assertThat(fieldNamingStrategy.getClass()).isEqualTo(CamelCaseAbbreviatingFieldNamingStrategy.class);
			});
	}

	@Test
	void defaultAutoIndexCreation() {
		this.contextRunner.run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			assertThat(mappingContext.isAutoIndexCreation()).isFalse();
		});
	}

	@Test
	void customAutoIndexCreation() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.autoIndexCreation:true").run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			assertThat(mappingContext.isAutoIndexCreation()).isTrue();
		});
	}

	@Test
	void interfaceFieldNamingStrategy() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.field-naming-strategy:" + FieldNamingStrategy.class.getName())
			.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
	}

	@Test
	void entityScanShouldSetManagedTypes() {
		this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
			assertThat(managedTypes.toList()).containsOnly(City.class, Country.class);
		});

	}

	@Test
	void registersDefaultSimpleTypesWithMappingContext() {
		this.contextRunner.run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			MongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(Sample.class);
			MongoPersistentProperty dateProperty = entity.getPersistentProperty("date");
			assertThat(dateProperty.isEntity()).isFalse();
		});

	}

	@Test
	void backsOffIfMongoClientBeanIsNotPresent() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoDataAutoConfiguration.class));
		runner.run((context) -> assertThat(context).doesNotHaveBean(MongoTemplate.class));
	}

	@Test
	void createsMongoDatabaseFactoryForPreferredMongoClient() {
		this.contextRunner.run((context) -> {
			MongoDatabaseFactory dbFactory = context.getBean(MongoDatabaseFactory.class);
			assertThat(dbFactory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
		});
	}

	@Test
	void createsMongoDatabaseFactoryForFallbackMongoClient() {
		this.contextRunner.withUserConfiguration(FallbackMongoClientConfiguration.class).run((context) -> {
			MongoDatabaseFactory dbFactory = context.getBean(MongoDatabaseFactory.class);
			assertThat(dbFactory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
		});
	}

	@Test
	void autoConfiguresIfUserProvidesMongoDatabaseFactoryButNoClient() {
		this.contextRunner.withUserConfiguration(MongoDatabaseFactoryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(MongoTemplate.class));
	}

	@Test
	void databaseHasDefault() {
		this.contextRunner.run((context) -> {
			MongoDatabaseFactory factory = context.getBean(MongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
			assertThat(factory.getMongoDatabase().getName()).isEqualTo("test");
		});
	}

	@Test
	void databasePropertyIsUsed() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.database=mydb").run((context) -> {
			MongoDatabaseFactory factory = context.getBean(MongoDatabaseFactory.class);
			assertThat(factory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
			assertThat(factory.getMongoDatabase().getName()).isEqualTo("mydb");
		});
	}

	@Test
	void databaseInUriPropertyIsUsed() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/mydb")
			.run((context) -> {
				MongoDatabaseFactory factory = context.getBean(MongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
				assertThat(factory.getMongoDatabase().getName()).isEqualTo("mydb");
			});
	}

	@Test
	void databasePropertyOverridesUriProperty() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/notused",
					"spring.data.mongodb.database=mydb")
			.run((context) -> {
				MongoDatabaseFactory factory = context.getBean(MongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
				assertThat(factory.getMongoDatabase().getName()).isEqualTo("mydb");
			});
	}

	@Test
	void databasePropertyIsUsedWhenNoDatabaseInUri() {
		this.contextRunner
			.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/",
					"spring.data.mongodb.database=mydb")
			.run((context) -> {
				MongoDatabaseFactory factory = context.getBean(MongoDatabaseFactory.class);
				assertThat(factory).isInstanceOf(SimpleMongoClientDatabaseFactory.class);
				assertThat(factory.getMongoDatabase().getName()).isEqualTo("mydb");
			});
	}

	@Test
	void contextFailsWhenDatabaseNotSet() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=mongodb://mongo.example.com/")
			.run((context) -> assertThat(context).getFailure().hasMessageContaining("Database name must not be empty"));
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
				return new ConnectionString("mongodb://localhost/testdb");
			}

		})
			.run((context) -> assertThat(context).hasSingleBean(MongoConnectionDetails.class)
				.doesNotHaveBean(PropertiesMongoConnectionDetails.class));
	}

	private static void assertDomainTypesDiscovered(MongoMappingContext mappingContext, Class<?>... types) {
		ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
		assertThat(managedTypes.toList()).containsOnly(types);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConversionsConfig {

		@Bean
		MongoCustomConversions customConversions() {
			return new MongoCustomConversions(Arrays.asList(new MyConverter()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.autoconfigure.data.mongo")
	static class EntityScanConfig {

	}

	@Configuration(proxyBeanMethods = false)
	static class FallbackMongoClientConfiguration {

		@Bean
		com.mongodb.client.MongoClient fallbackMongoClient() {
			return MongoClients.create();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MongoDatabaseFactoryConfiguration {

		@Bean
		MongoDatabaseFactory mongoDatabaseFactory() {
			return new SimpleMongoClientDatabaseFactory(MongoClients.create(), "test");
		}

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
					return GridFs.of("grid-database-1", "connection-details-bucket");
				}

			};
		}

	}

	static class MyConverter implements Converter<MongoClient, Boolean> {

		@Override
		public Boolean convert(MongoClient source) {
			return null;
		}

	}

	static class Sample {

		LocalDateTime date;

	}

}
