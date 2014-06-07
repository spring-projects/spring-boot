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

package org.springframework.boot.autoconfigure.data.mongo;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.data.jpa.city.CityRepository;
import org.springframework.boot.autoconfigure.data.mongo.country.Country;
import org.springframework.boot.autoconfigure.data.mongo.country.CountryRepository;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfigurationTests;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link MongoRepositoriesAutoConfiguration}.
 * 
 * @author Dave Syer
 * @author Oliver Gierke
 */
public class MixedMongoRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false");
		this.context.register(TestConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(CountryRepository.class));
	}

	@Test
	public void testMixedRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false");
		this.context.register(MixedConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(CountryRepository.class));
		assertNotNull(this.context.getBean(CityRepository.class));
	}

	@Configuration
	@Import({ MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
			MongoRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(MongoAutoConfigurationTests.class)
	// Not this package or its parent
	@EnableMongoRepositories(basePackageClasses = Country.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(MongoAutoConfigurationTests.class)
	@EnableMongoRepositories(basePackageClasses = Country.class)
	@EntityScan(basePackageClasses = City.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	protected static class MixedConfiguration {

	}
}
