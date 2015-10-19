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

package org.springframework.boot.autoconfigure.data.cassandra;

import com.datastax.driver.core.Session;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.cassandra.core.CassandraTemplate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraDataAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
public class CassandraDataAutoConfigurationTests {

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
		this.context.register(TestExcludeConfiguration.class, TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(CassandraTemplate.class).length);
	}

	@Configuration
	@ComponentScan(excludeFilters = @ComponentScan.Filter(classes = {
			Session.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class TestExcludeConfiguration {

	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public Session getObject() {
			return mock(Session.class);
		}

	}

}
