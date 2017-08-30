/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.mongo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import com.mongodb.Mongo;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.city.City;
import org.springframework.boot.autoconfigure.data.mongo.country.Country;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link MongoDataAutoConfiguration}.
 *
 * @author Josh Long
 * @author Oliver Gierke
 */
public class MongoDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void templateExists() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(MongoTemplate.class).length)
				.isEqualTo(1);
	}

	@Test
	public void gridFsTemplateExists() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.gridFsDatabase:grid")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(GridFsTemplate.class).length)
				.isEqualTo(1);
	}

	@Test
	public void customConversions() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CustomConversionsConfig.class);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		this.context.refresh();
		MongoTemplate template = this.context.getBean(MongoTemplate.class);
		assertThat(template.getConverter().getConversionService().canConvert(Mongo.class,
				Boolean.class)).isTrue();
	}

	@Test
	public void usesAutoConfigurationPackageToPickUpDocumentTypes() {
		this.context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(this.context, cityPackage);
		this.context.register(MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class);
		this.context.refresh();
		assertDomainTypesDiscovered(this.context.getBean(MongoMappingContext.class),
				City.class);
	}

	@Test
	public void defaultFieldNamingStrategy() {
		testFieldNamingStrategy(null, PropertyNameFieldNamingStrategy.class);
	}

	@Test
	public void customFieldNamingStrategy() {
		testFieldNamingStrategy(CamelCaseAbbreviatingFieldNamingStrategy.class.getName(),
				CamelCaseAbbreviatingFieldNamingStrategy.class);
	}

	@Test
	public void interfaceFieldNamingStrategy() {
		try {
			testFieldNamingStrategy(FieldNamingStrategy.class.getName(), null);
			fail("Create FieldNamingStrategy interface should fail");
		}
		// We seem to have an inconsistent exception, accept either
		catch (BeanCreationException ex) {
			// Expected
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void entityScanShouldSetInitialEntitySet() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EntityScanConfig.class,
				PropertyPlaceholderAutoConfiguration.class, MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class);
		this.context.refresh();
		MongoMappingContext mappingContext = this.context
				.getBean(MongoMappingContext.class);
		Set<Class<?>> initialEntitySet = (Set<Class<?>>) ReflectionTestUtils
				.getField(mappingContext, "initialEntitySet");
		assertThat(initialEntitySet).containsOnly(City.class, Country.class);
	}

	@Test
	public void registersDefaultSimpleTypesWithMappingContext() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class);
		this.context.refresh();
		MongoMappingContext context = this.context.getBean(MongoMappingContext.class);
		BasicMongoPersistentEntity<?> entity = context.getPersistentEntity(Sample.class);
		MongoPersistentProperty dateProperty = entity.getPersistentProperty("date");
		assertThat(dateProperty.isEntity()).isFalse();
	}

	public void testFieldNamingStrategy(String strategy,
			Class<? extends FieldNamingStrategy> expectedType) {
		this.context = new AnnotationConfigApplicationContext();
		if (strategy != null) {
			TestPropertyValues.of("spring.data.mongodb.field-naming-strategy:" + strategy)
					.applyTo(this.context);
		}
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		this.context.refresh();
		MongoMappingContext mappingContext = this.context
				.getBean(MongoMappingContext.class);
		FieldNamingStrategy fieldNamingStrategy = (FieldNamingStrategy) ReflectionTestUtils
				.getField(mappingContext, "fieldNamingStrategy");
		assertThat(fieldNamingStrategy.getClass()).isEqualTo(expectedType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertDomainTypesDiscovered(MongoMappingContext mappingContext,
			Class<?>... types) {
		Set<Class> initialEntitySet = (Set<Class>) ReflectionTestUtils
				.getField(mappingContext, "initialEntitySet");
		assertThat(initialEntitySet).containsOnly(types);
	}

	@Configuration
	static class CustomConversionsConfig {

		@Bean
		public MongoCustomConversions customConversions() {
			return new MongoCustomConversions(Arrays.asList(new MyConverter()));
		}

	}

	@Configuration
	@EntityScan("org.springframework.boot.autoconfigure.data.mongo")
	static class EntityScanConfig {

	}

	private static class MyConverter implements Converter<Mongo, Boolean> {

		@Override
		public Boolean convert(Mongo source) {
			return null;
		}

	}

	static class Sample {

		LocalDateTime date;

	}

}
