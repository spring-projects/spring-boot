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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.alt.elasticsearch.CityElasticsearchDbRepository;
import org.springframework.boot.autoconfigure.data.elasticsearch.city.City;
import org.springframework.boot.autoconfigure.data.elasticsearch.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link ElasticsearchRepositoriesAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class ElasticsearchRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				ElasticsearchAutoConfiguration.class,
				ElasticsearchRepositoriesAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(CityRepository.class));
		assertNotNull(this.context.getBean(Client.class));
	}

	@Test
	public void testNoRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmptyConfiguration.class,
				ElasticsearchAutoConfiguration.class,
				ElasticsearchRepositoriesAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(Client.class));
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CustomizedConfiguration.class,
				ElasticsearchAutoConfiguration.class,
				ElasticsearchRepositoriesAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(CityElasticsearchDbRepository.class));
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	protected static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(ElasticsearchRepositoriesAutoConfigurationTests.class)
	@EnableElasticsearchRepositories(basePackageClasses = CityElasticsearchDbRepository.class)
	protected static class CustomizedConfiguration {

	}
}
