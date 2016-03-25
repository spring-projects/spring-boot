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

import org.assertj.core.api.Assertions;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.template.Neo4jOperations;

/**
 * Tests for {@link Neo4jAutoConfiguration}.
 *
 * @author Josh Long
 * @author Oliver Gierke
 * @author Vince Bickers
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
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(PropertyPlaceholderAutoConfiguration.class, Neo4jAutoConfiguration.class);
		this.context.refresh();
		Assertions.assertThat(this.context.getBeanNamesForType(Neo4jOperations.class).length).isEqualTo(1);
	}

	@Test
	public void sessionFactoryExists() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(PropertyPlaceholderAutoConfiguration.class, Neo4jAutoConfiguration.class);
		this.context.refresh();
		Assertions.assertThat(this.context.getBeanNamesForType(SessionFactory.class).length).isEqualTo(1);
	}

	@Test
	public void usesAutoConfigurationPackageToPickUpDomainTypes() {
		this.context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(this.context, cityPackage);
		this.context.register(Neo4jAutoConfiguration.class);
		this.context.refresh();
		assertDomainTypesDiscovered(this.context.getBean(Neo4jMappingContext.class),
				City.class);
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertDomainTypesDiscovered(Neo4jMappingContext mappingContext,
			Class<?>... types) {
		for (Class<?> type : types) {
			Assertions.assertThat(mappingContext.getPersistentEntity(type)).isNotNull();
		}
	}

}
