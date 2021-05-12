/*
 * Copyright 2012-2021 the original author or authors.
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

import java.math.BigDecimal;
import java.util.Collections;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchDataAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Artur Konczak
 * @author Brian Clozel
 * @author Peter-Josef Meisch
 * @author Scott Frederick
 * @author Stephane Nicoll
 */
class ElasticsearchDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class,
					ReactiveElasticsearchRestClientAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class));

	@BeforeEach
	void setUp() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	@AfterEach
	void tearDown() {
		System.clearProperty("es.set.netty.runtime.available.processors");
	}

	@Test
	void defaultRestBeansRegistered() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ElasticsearchRestTemplate.class)
				.hasSingleBean(ReactiveElasticsearchTemplate.class).hasSingleBean(ElasticsearchConverter.class)
				.hasSingleBean(ElasticsearchConverter.class).hasSingleBean(ElasticsearchCustomConversions.class));
	}

	@Test
	void defaultConversionsRegisterBigDecimalAsSimpleType() {
		this.contextRunner.run((context) -> {
			SimpleElasticsearchMappingContext mappingContext = context.getBean(SimpleElasticsearchMappingContext.class);
			assertThat(mappingContext)
					.extracting("simpleTypeHolder", InstanceOfAssertFactories.type(SimpleTypeHolder.class)).satisfies(
							(simpleTypeHolder) -> assertThat(simpleTypeHolder.isSimpleType(BigDecimal.class)).isTrue());
		});
	}

	@Test
	void customConversionsShouldBeUsed() {
		this.contextRunner.withUserConfiguration(CustomElasticsearchCustomConversions.class).run((context) -> {
			assertThat(context).hasSingleBean(ElasticsearchCustomConversions.class).hasBean("testCustomConversions");
			assertThat(context.getBean(ElasticsearchConverter.class).getConversionService()
					.canConvert(ElasticsearchRestTemplate.class, Boolean.class)).isTrue();
		});
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
	static class CustomElasticsearchCustomConversions {

		@Bean
		ElasticsearchCustomConversions testCustomConversions() {
			return new ElasticsearchCustomConversions(Collections.singletonList(new MyConverter()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestTemplate {

		@Bean
		ElasticsearchRestTemplate elasticsearchTemplate() {
			return mock(ElasticsearchRestTemplate.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomReactiveRestTemplate {

		@Bean
		ReactiveElasticsearchTemplate reactiveElasticsearchTemplate() {
			return mock(ReactiveElasticsearchTemplate.class);
		}

	}

	static class MyConverter implements Converter<ElasticsearchRestTemplate, Boolean> {

		@Override
		public Boolean convert(ElasticsearchRestTemplate source) {
			return null;
		}

	}

}
