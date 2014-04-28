/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.boot.autoconfigure.data;

import static org.hamcrest.core.IsInstanceOf.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.alt.CitySolrRepository;
import org.springframework.boot.autoconfigure.data.solr.City;
import org.springframework.boot.autoconfigure.data.solr.CityRepository;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

/**
 * @author Christoph Strobl
 */
public class SolrRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultRepositoryConfiguration() {

		initContext(TestConfiguration.class);

		assertThat(this.context.getBean(CityRepository.class), notNullValue());
		assertThat(this.context.getBean(SolrServer.class), instanceOf(HttpSolrServer.class));
	}

	@Test
	public void testNoRepositoryConfiguration() {

		initContext(EmptyConfiguration.class);
		assertThat(this.context.getBean(SolrServer.class), instanceOf(HttpSolrServer.class));
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {

		initContext(CustomizedConfiguration.class);
		assertThat(this.context.getBean(CitySolrRepository.class), notNullValue());
	}

	private void initContext(Class<?> configClass) {

		this.context = new AnnotationConfigApplicationContext();
		this.context.register(configClass, SolrAutoConfiguration.class, SolrRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(SolrRepositoriesAutoConfigurationTests.class)
	static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(SolrRepositoriesAutoConfigurationTests.class)
	@EnableSolrRepositories(basePackageClasses = CitySolrRepository.class, multicoreSupport = true)
	protected static class CustomizedConfiguration {

	}

}
