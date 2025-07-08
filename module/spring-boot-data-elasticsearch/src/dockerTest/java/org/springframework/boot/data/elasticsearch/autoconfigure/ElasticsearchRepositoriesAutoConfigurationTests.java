/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.elasticsearch.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.data.elasticsearch.domain.city.City;
import org.springframework.boot.data.elasticsearch.domain.city.CityRepository;
import org.springframework.boot.data.elasticsearch.domain.empty.EmptyDataPackage;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchRepositoriesAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchRepositoriesAutoConfigurationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = TestImage.container(ElasticsearchContainer.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class,
				ElasticsearchClientAutoConfiguration.class, ElasticsearchRepositoriesAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class))
		.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress());

	@Test
	void testDefaultRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(CityRepository.class)
				.hasSingleBean(ElasticsearchTemplate.class));
	}

	@Test
	void testNoRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ElasticsearchTemplate.class));
	}

	@Test
	void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		this.contextRunner.withUserConfiguration(CustomizedConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(CityRepository.class));
	}

	@Test
	void testAuditingConfiguration() {
		this.contextRunner.withUserConfiguration(AuditingConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ElasticsearchTemplate.class));
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@EnableElasticsearchRepositories(basePackageClasses = CityRepository.class)
	static class CustomizedConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@EnableElasticsearchRepositories
	@EnableElasticsearchAuditing
	static class AuditingConfiguration {

	}

}
