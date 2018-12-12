/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.web;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringDataWebAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class SpringDataWebAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(SpringDataWebAutoConfiguration.class));

	@Test
	public void webSupportIsAutoConfiguredInWebApplicationContexts() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(PageableHandlerMethodArgumentResolver.class));
	}

	@Test
	public void autoConfigurationBacksOffInNonWebApplicationContexts() {
		new ApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(SpringDataWebAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.doesNotHaveBean(PageableHandlerMethodArgumentResolver.class));
	}

	@Test
	public void customizePageable() {
		this.contextRunner
				.withPropertyValues("spring.data.web.pageable.page-parameter=p",
						"spring.data.web.pageable.size-parameter=s",
						"spring.data.web.pageable.default-page-size=10",
						"spring.data.web.pageable.prefix=abc",
						"spring.data.web.pageable.qualifier-delimiter=__",
						"spring.data.web.pageable.max-page-size=100",
						"spring.data.web.pageable.one-indexed-parameters=true")
				.run((context) -> {
					PageableHandlerMethodArgumentResolver argumentResolver = context
							.getBean(PageableHandlerMethodArgumentResolver.class);
					assertThat(argumentResolver)
							.hasFieldOrPropertyWithValue("pageParameterName", "p");
					assertThat(argumentResolver)
							.hasFieldOrPropertyWithValue("sizeParameterName", "s");
					assertThat(argumentResolver)
							.hasFieldOrPropertyWithValue("oneIndexedParameters", true);
					assertThat(argumentResolver).hasFieldOrPropertyWithValue("prefix",
							"abc");
					assertThat(argumentResolver)
							.hasFieldOrPropertyWithValue("qualifierDelimiter", "__");
					assertThat(argumentResolver).hasFieldOrPropertyWithValue(
							"fallbackPageable", PageRequest.of(0, 10));
					assertThat(argumentResolver)
							.hasFieldOrPropertyWithValue("maxPageSize", 100);
				});
	}

	@Test
	public void defaultPageable() {
		this.contextRunner.run((context) -> {
			SpringDataWebProperties.Pageable properties = new SpringDataWebProperties()
					.getPageable();
			PageableHandlerMethodArgumentResolver argumentResolver = context
					.getBean(PageableHandlerMethodArgumentResolver.class);
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("pageParameterName",
					properties.getPageParameter());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("sizeParameterName",
					properties.getSizeParameter());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue(
					"oneIndexedParameters", properties.isOneIndexedParameters());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("prefix",
					properties.getPrefix());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("qualifierDelimiter",
					properties.getQualifierDelimiter());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("fallbackPageable",
					PageRequest.of(0, properties.getDefaultPageSize()));
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("maxPageSize",
					properties.getMaxPageSize());
		});
	}

	@Test
	public void customizeSort() {
		this.contextRunner.withPropertyValues("spring.data.web.sort.sort-parameter=s")
				.run((context) -> {
					SortHandlerMethodArgumentResolver argumentResolver = context
							.getBean(SortHandlerMethodArgumentResolver.class);
					assertThat(argumentResolver)
							.hasFieldOrPropertyWithValue("sortParameter", "s");
				});
	}

}
