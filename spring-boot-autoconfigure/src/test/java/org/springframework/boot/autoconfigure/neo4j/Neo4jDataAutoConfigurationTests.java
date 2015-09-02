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

package org.springframework.boot.autoconfigure.neo4j;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.springframework.boot.autoconfigure.neo4j.Neo4jDataAutoConfiguration}.
 *
 * @author Josh Long
 * @author Oliver Gierke
 */
public class Neo4jDataAutoConfigurationTests {

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
				PropertyPlaceholderAutoConfiguration.class, Neo4jDataAutoConfiguration.class);
		assertEquals(1, this.context.getBeansOfType(Neo4jOperations.class).size());
	}

	@Test
	public void sessionFactoryExists() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(PropertyPlaceholderAutoConfiguration.class, Neo4jDataAutoConfiguration.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(SessionFactory.class).length);
	}

	@Test
	public void customConversions() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(PropertyPlaceholderAutoConfiguration.class, Neo4jDataAutoConfiguration.class);
		this.context.register(CustomConversionsConfig.class);
		this.context.refresh();
		Neo4jSession template = this.context.getBean(Neo4jSession.class);
		// TODO
/*
		assertTrue(template.context().getConversionService()
				.canConvert(Neo4jServer.class, Boolean.class));
*/
	}

	@Test
	public void usesAutoConfigurationPackageToPickUpDocumentTypes() {
		this.context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(this.context, cityPackage);
		this.context.register(Neo4jDataAutoConfiguration.class);
		this.context.refresh();
		assertDomainTypesDiscovered(this.context.getBean(Neo4jMappingContext.class),
				City.class);
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertDomainTypesDiscovered(Neo4jMappingContext mappingContext,
			Class<?>... types) {
		for (Class<?> type : types) {
			assertThat(mappingContext.getPersistentEntity(type), is(notNullValue()));
		}
	}

	@Configuration
	static class CustomConversionsConfig {

		@Bean
		public ConversionService conversionService(SessionFactory factory) {
			return new MetaDataDrivenConversionService(factory.metaData());
		}

		@Bean
		public CustomConversions customConversions() {
			return new CustomConversions(Arrays.asList(new MyConverter()));
		}
	}

	private static class MyConverter implements Converter<Neo4jServer, Boolean> {

		@Override
		public Boolean convert(Neo4jServer source) {
			return Boolean.FALSE;
		}

	}

}
