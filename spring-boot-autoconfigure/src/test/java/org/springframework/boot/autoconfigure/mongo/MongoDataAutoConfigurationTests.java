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

package org.springframework.boot.autoconfigure.mongo;

import java.util.Arrays;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.city.City;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.mongodb.Mongo;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link MongoDataAutoConfiguration}.
 *
 * @author Josh Long
 * @author Oliver Gierke
 */
public class MongoDataAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

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
		assertEquals(1, this.context.getBeanNamesForType(MongoTemplate.class).length);
	}

	@Test
	public void gridFsTemplateExists() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.mongodb.gridFsDatabase:grid");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(GridFsTemplate.class).length);
	}

	@Test
	public void customConversions() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CustomConversionsConfig.class);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		this.context.refresh();
		MongoTemplate template = this.context.getBean(MongoTemplate.class);
		assertTrue(template.getConverter().getConversionService()
				.canConvert(Mongo.class, Boolean.class));
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
		thrown.expectMessage("Invalid custom FieldNamingStrategy");
		testFieldNamingStrategy(FieldNamingStrategy.class.getName(), null);
	}

	public void testFieldNamingStrategy(String strategy, Class<? extends FieldNamingStrategy> expectedType) {
		this.context = new AnnotationConfigApplicationContext();
		if (strategy != null) {
			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.data.mongodb.field-naming-strategy:" + strategy);
		}
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class);
		this.context.refresh();
		MongoMappingContext mappingContext = this.context.getBean(MongoMappingContext.class);
		FieldNamingStrategy fieldNamingStrategy =
				(FieldNamingStrategy) ReflectionTestUtils.getField(mappingContext, "fieldNamingStrategy");
		assertEquals(expectedType, fieldNamingStrategy.getClass());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertDomainTypesDiscovered(MongoMappingContext mappingContext,
			Class<?>... types) {
		Set<Class> initialEntitySet = (Set<Class>) ReflectionTestUtils.getField(
				mappingContext, "initialEntitySet");
		assertThat(initialEntitySet, hasSize(types.length));
		assertThat(initialEntitySet, Matchers.<Class> hasItems(types));
	}

	@Configuration
	static class CustomConversionsConfig {

		@Bean
		public CustomConversions customConversions() {
			return new CustomConversions(Arrays.asList(new MyConverter()));
		}
	}

	private static class MyConverter implements Converter<Mongo, Boolean> {

		@Override
		public Boolean convert(Mongo source) {
			return null;
		}

	}

}
