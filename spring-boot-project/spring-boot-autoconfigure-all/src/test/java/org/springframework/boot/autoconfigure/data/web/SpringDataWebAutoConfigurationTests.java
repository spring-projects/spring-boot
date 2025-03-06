/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.web;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.data.web.config.SpringDataWebSettings;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringDataWebAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Yanming Zhou
 */
class SpringDataWebAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SpringDataWebAutoConfiguration.class));

	@Test
	void webSupportIsAutoConfiguredInWebApplicationContexts() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(PageableHandlerMethodArgumentResolver.class));
	}

	@Test
	void autoConfigurationBacksOffInNonWebApplicationContexts() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SpringDataWebAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(PageableHandlerMethodArgumentResolver.class));
	}

	@Test
	void customizePageable() {
		this.contextRunner
			.withPropertyValues("spring.data.web.pageable.page-parameter=p",
					"spring.data.web.pageable.size-parameter=s", "spring.data.web.pageable.default-page-size=10",
					"spring.data.web.pageable.prefix=abc", "spring.data.web.pageable.qualifier-delimiter=__",
					"spring.data.web.pageable.max-page-size=100", "spring.data.web.pageable.serialization-mode=VIA_DTO",
					"spring.data.web.pageable.one-indexed-parameters=true")
			.run((context) -> {
				PageableHandlerMethodArgumentResolver argumentResolver = context
					.getBean(PageableHandlerMethodArgumentResolver.class);
				SpringDataWebSettings springDataWebSettings = context.getBean(SpringDataWebSettings.class);
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("pageParameterName", "p");
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("sizeParameterName", "s");
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("oneIndexedParameters", true);
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("prefix", "abc");
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("qualifierDelimiter", "__");
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("fallbackPageable", PageRequest.of(0, 10));
				assertThat(argumentResolver).hasFieldOrPropertyWithValue("maxPageSize", 100);
				assertThat(springDataWebSettings.pageSerializationMode()).isEqualTo(PageSerializationMode.VIA_DTO);
			});
	}

	@Test
	void defaultPageable() {
		this.contextRunner.run((context) -> {
			SpringDataWebProperties.Pageable properties = new SpringDataWebProperties().getPageable();
			PageableHandlerMethodArgumentResolver argumentResolver = context
				.getBean(PageableHandlerMethodArgumentResolver.class);
			SpringDataWebSettings springDataWebSettings = context.getBean(SpringDataWebSettings.class);
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("pageParameterName",
					properties.getPageParameter());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("sizeParameterName",
					properties.getSizeParameter());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("oneIndexedParameters",
					properties.isOneIndexedParameters());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("prefix", properties.getPrefix());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("qualifierDelimiter",
					properties.getQualifierDelimiter());
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("fallbackPageable",
					PageRequest.of(0, properties.getDefaultPageSize()));
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("maxPageSize", properties.getMaxPageSize());
			assertThat(springDataWebSettings.pageSerializationMode()).isEqualTo(properties.getSerializationMode());
		});
	}

	@Test
	void customizeSort() {
		this.contextRunner.withPropertyValues("spring.data.web.sort.sort-parameter=s").run((context) -> {
			SortHandlerMethodArgumentResolver argumentResolver = context
				.getBean(SortHandlerMethodArgumentResolver.class);
			assertThat(argumentResolver).hasFieldOrPropertyWithValue("sortParameter", "s");
		});
	}

	@Test
	void customizePageSerializationModeViaConfigProps() {
		this.contextRunner.withPropertyValues("spring.data.web.pageable.serialization-mode=VIA_DTO").run((context) -> {
			SpringDataWebSettings springDataWebSettings = context.getBean(SpringDataWebSettings.class);
			assertThat(springDataWebSettings.pageSerializationMode()).isEqualTo(PageSerializationMode.VIA_DTO);
		});
	}

	@Test
	void customizePageSerializationModeViaCustomBean() {
		this.contextRunner
			.withBean("customSpringDataWebSettings", SpringDataWebSettings.class,
					() -> new SpringDataWebSettings(PageSerializationMode.VIA_DTO))
			.run((context) -> {
				assertThat(context).doesNotHaveBean("springDataWebSettings");
				SpringDataWebSettings springDataWebSettings = context.getBean(SpringDataWebSettings.class);
				assertThat(springDataWebSettings.pageSerializationMode()).isEqualTo(PageSerializationMode.VIA_DTO);
			});
	}

}
