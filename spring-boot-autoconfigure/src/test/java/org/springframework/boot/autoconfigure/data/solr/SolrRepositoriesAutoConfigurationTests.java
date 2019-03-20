/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.solr.CitySolrRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.solr.city.City;
import org.springframework.boot.autoconfigure.data.solr.city.CityRepository;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SolrRepositoriesAutoConfiguration}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class SolrRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() {
		initContext(TestConfiguration.class);
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		assertThat(this.context.getBean(SolrClient.class))
				.isInstanceOf(HttpSolrClient.class);
	}

	@Test
	public void testNoRepositoryConfiguration() {
		initContext(EmptyConfiguration.class);
		assertThat(this.context.getBean(SolrClient.class))
				.isInstanceOf(HttpSolrClient.class);
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		initContext(CustomizedConfiguration.class);
		assertThat(this.context.getBean(CitySolrRepository.class)).isNotNull();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {

		initContext(SortOfInvalidCustomConfiguration.class);
		this.context.getBean(CityRepository.class);
	}

	private void initContext(Class<?> configClass) {

		this.context = new AnnotationConfigApplicationContext();
		this.context.register(configClass, SolrAutoConfiguration.class,
				SolrRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(SolrRepositoriesAutoConfigurationTests.class)
	@EnableSolrRepositories(basePackageClasses = CitySolrRepository.class, multicoreSupport = true)
	protected static class CustomizedConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(SolrRepositoriesAutoConfigurationTests.class)
	// To not find any repositories
	@EnableSolrRepositories("foo.bar")
	protected static class SortOfInvalidCustomConfiguration {

	}

}
