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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.elasticsearch.CityElasticsearchDbRepository;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchNodeTemplate.ElasticsearchNode;
import org.springframework.boot.autoconfigure.data.elasticsearch.city.City;
import org.springframework.boot.autoconfigure.data.elasticsearch.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.runner.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchRepositoriesAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides("org.apache.logging.log4j:log4j-core:2.10.0")
public class ElasticsearchRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() {
		new ElasticsearchNodeTemplate().doWithNode((node) -> {
			load(TestConfiguration.class, node);
			assertThat(this.context.getBean(CityRepository.class)).isNotNull();
			assertThat(this.context.getBean(Client.class)).isNotNull();
		});

	}

	@Test
	public void testNoRepositoryConfiguration() {
		new ElasticsearchNodeTemplate().doWithNode((node) -> {
			load(EmptyConfiguration.class, node);
			assertThat(this.context.getBean(Client.class)).isNotNull();
		});
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		new ElasticsearchNodeTemplate().doWithNode((node) -> {
			load(CustomizedConfiguration.class, node);
			assertThat(this.context.getBean(CityElasticsearchDbRepository.class))
					.isNotNull();
		});
	}

	private void load(Class<?> config, ElasticsearchNode node) {
		this.context = new AnnotationConfigApplicationContext();
		addElasticsearchProperties(this.context, node);
		this.context.register(config, ElasticsearchAutoConfiguration.class,
				ElasticsearchRepositoriesAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	private void addElasticsearchProperties(AnnotationConfigApplicationContext context,
			ElasticsearchNode node) {
		TestPropertyValues.of("spring.data.elasticsearch.properties.path.home:target",
				"spring.data.elasticsearch.cluster-nodes:localhost:" + node.getTcpPort())
				.applyTo(context);
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
