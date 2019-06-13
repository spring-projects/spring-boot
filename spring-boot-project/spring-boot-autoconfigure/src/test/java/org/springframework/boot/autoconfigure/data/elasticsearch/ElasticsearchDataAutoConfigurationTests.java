/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DisabledWithoutDockerTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchDataAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Artur Konczak
 * @author Brian Clozel
 */
@DisabledWithoutDockerTestcontainers
class ElasticsearchDataAutoConfigurationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer();

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ElasticsearchAutoConfiguration.class, RestClientAutoConfiguration.class,
					ReactiveRestClientAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class));

	@BeforeEach
	public void setUp() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	@AfterEach
	public void tearDown() {
		System.clearProperty("es.set.netty.runtime.available.processors");
	}

	@Test
	void defaultTransportBeansAreRegistered() {
		this.contextRunner
				.withPropertyValues(
						"spring.data.elasticsearch.cluster-nodes:" + elasticsearch.getTcpHost().getHostString() + ":"
								+ elasticsearch.getTcpHost().getPort(),
						"spring.data.elasticsearch.cluster-name:docker-cluster")
				.run((context) -> assertThat(context).hasSingleBean(ElasticsearchTemplate.class)
						.hasSingleBean(SimpleElasticsearchMappingContext.class)
						.hasSingleBean(ElasticsearchConverter.class));
	}

	@Test
	void defaultTransportBeansNotRegisteredIfNoTransportClient() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchTemplate.class));
	}

	@Test
	void defaultRestBeansRegistered() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ElasticsearchRestTemplate.class)
				.hasSingleBean(ReactiveElasticsearchTemplate.class).hasSingleBean(ElasticsearchConverter.class)
				.hasSingleBean(SimpleElasticsearchMappingContext.class).hasSingleBean(EntityMapper.class)
				.hasSingleBean(ElasticsearchConverter.class));
	}

	@Test
	void customTransportTemplateShouldBeUsed() {
		this.contextRunner.withUserConfiguration(CustomTransportTemplate.class).run((context) -> assertThat(context)
				.getBeanNames(ElasticsearchTemplate.class).hasSize(1).contains("elasticsearchTemplate"));
	}

	@Test
	void customRestTemplateShouldBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRestTemplate.class).run((context) -> assertThat(context)
				.getBeanNames(ElasticsearchRestTemplate.class).hasSize(1).contains("elasticsearchTemplate"));
	}

	@Test
	void customReactiveRestTemplateShouldBeUsed() {
		this.contextRunner.withUserConfiguration(CustomReactiveRestTemplate.class)
				.run((context) -> assertThat(context).getBeanNames(ReactiveElasticsearchTemplate.class).hasSize(1)
						.contains("reactiveElasticsearchTemplate"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTransportTemplate {

		@Bean
		public ElasticsearchTemplate elasticsearchTemplate() {
			return mock(ElasticsearchTemplate.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestTemplate {

		@Bean
		public ElasticsearchRestTemplate elasticsearchTemplate() {
			return mock(ElasticsearchRestTemplate.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomReactiveRestTemplate {

		@Bean
		public ReactiveElasticsearchTemplate reactiveElasticsearchTemplate() {
			return mock(ReactiveElasticsearchTemplate.class);
		}

	}

}
