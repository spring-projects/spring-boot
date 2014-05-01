/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.googlecode.flyway.core.Flyway;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link LiquibaseAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class FlywayAutoConfigurationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();;

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:flywaytest");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testNoDataSource() throws Exception {
		this.context.register(FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(Flyway.class).length);
	}

	@Test
	public void testDefaultFlyway() throws Exception {
		this.context
				.register(EmbeddedDataSourceConfiguration.class,
						FlywayAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		Flyway flyway = this.context.getBean(Flyway.class);
		assertEquals("[classpath:db/migrations]", Arrays.asList(flyway.getLocations())
				.toString());
	}

	@Test
	public void testOverrideLocations() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"flyway.locations:classpath:db/changelog");
		this.context
				.register(EmbeddedDataSourceConfiguration.class,
						FlywayAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		Flyway flyway = this.context.getBean(Flyway.class);
		assertEquals("[classpath:db/changelog]", Arrays.asList(flyway.getLocations())
				.toString());
	}

	@Test
	public void testOverrideSchemas() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "flyway.schemas:public");
		this.context
				.register(EmbeddedDataSourceConfiguration.class,
						FlywayAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		Flyway flyway = this.context.getBean(Flyway.class);
		assertEquals("[public]", Arrays.asList(flyway.getSchemas()).toString());
	}

	@Test(expected = BeanCreationException.class)
	public void testChangeLogDoesNotExist() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "flyway.locations:no-such-dir");
		this.context
				.register(EmbeddedDataSourceConfiguration.class,
						FlywayAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}
}
