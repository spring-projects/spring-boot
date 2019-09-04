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

package org.springframework.boot.autoconfigure.data.mongo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.city.City;
import org.springframework.boot.autoconfigure.data.mongo.country.Country;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoDataAutoConfiguration}.
 *
 * @author Josh Long
 * @author Oliver Gierke
 */
class MongoDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
					MongoAutoConfiguration.class, MongoDataAutoConfiguration.class))
			.withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.INFO));

	@Test
	void templateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoTemplate.class));
	}

	@Test
	void gridFsTemplateExists() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridFsDatabase:grid")
				.run((context) -> assertThat(context).hasSingleBean(GridFsTemplate.class));
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
		this.contextRunner.withPropertyValues(
				"spring.data.mongodb.field-naming-strategy:" + CamelCaseAbbreviatingFieldNamingStrategy.class.getName())
				.run((context) -> {
					MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
					FieldNamingStrategy fieldNamingStrategy = (FieldNamingStrategy) ReflectionTestUtils
							.getField(mappingContext, "fieldNamingStrategy");
					assertThat(fieldNamingStrategy.getClass())
							.isEqualTo(CamelCaseAbbreviatingFieldNamingStrategy.class);
				});
	}

	@Test
	void customAutoIndexCreation() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.autoIndexCreation:false").run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			assertThat(mappingContext.isAutoIndexCreation()).isFalse();
		});
	}

	@Test
	void interfaceFieldNamingStrategy() {
		this.contextRunner
				.withPropertyValues("spring.data.mongodb.field-naming-strategy:" + FieldNamingStrategy.class.getName())
				.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void entityScanShouldSetInitialEntitySet() {
		this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			Set<Class<?>> initialEntitySet = (Set<Class<?>>) ReflectionTestUtils.getField(mappingContext,
					"initialEntitySet");
			assertThat(initialEntitySet).containsOnly(City.class, Country.class);
		});

	}

	@Test
	void registersDefaultSimpleTypesWithMappingContext() {
		this.contextRunner.run((context) -> {
			MongoMappingContext mappingContext = context.getBean(MongoMappingContext.class);
			BasicMongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(Sample.class);
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
	void createsMongoDbFactoryForPreferredMongoClient() {
		this.contextRunner.run((context) -> {
			MongoDbFactory dbFactory = context.getBean(MongoDbFactory.class);
			assertThat(dbFactory).isInstanceOf(SimpleMongoDbFactory.class);
		});
	}

	@Test
	void createsMongoDbFactoryForFallbackMongoClient() {
		this.contextRunner.withUserConfiguration(FallbackMongoClientConfiguration.class).run((context) -> {
			MongoDbFactory dbFactory = context.getBean(MongoDbFactory.class);
			assertThat(dbFactory).isInstanceOf(SimpleMongoClientDbFactory.class);
		});
	}

	@Test
	void autoConfiguresIfUserProvidesMongoDbFactoryButNoClient() {
		this.contextRunner.withUserConfiguration(MongoDbFactoryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(MongoTemplate.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertDomainTypesDiscovered(MongoMappingContext mappingContext, Class<?>... types) {
		Set<Class> initialEntitySet = (Set<Class>) ReflectionTestUtils.getField(mappingContext, "initialEntitySet");
		assertThat(initialEntitySet).containsOnly(types);
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
	static class MongoDbFactoryConfiguration {

		@Bean
		MongoDbFactory mongoDbFactory() {
			return new SimpleMongoClientDbFactory(MongoClients.create(), "test");
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
