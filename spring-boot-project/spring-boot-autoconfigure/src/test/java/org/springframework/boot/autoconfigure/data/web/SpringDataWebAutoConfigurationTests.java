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

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringDataWebAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class SpringDataWebAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void webSupportIsAutoConfiguredInWebApplicationContexts() {
		load();
		this.context.setServletContext(new MockServletContext());
		Map<String, PageableHandlerMethodArgumentResolver> beans = this.context
				.getBeansOfType(PageableHandlerMethodArgumentResolver.class);
		assertThat(beans).hasSize(1);
	}

	@Test
	public void autoConfigurationBacksOffInNonWebApplicationContexts() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SpringDataWebAutoConfiguration.class);
		try {
			ctx.refresh();
			Map<String, PageableHandlerMethodArgumentResolver> beans = ctx
					.getBeansOfType(PageableHandlerMethodArgumentResolver.class);
			assertThat(beans).isEmpty();
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void customizePageable() {
		load("spring.data.web.pageable.page-parameter=p",
				"spring.data.web.pageable.size-parameter=s",
				"spring.data.web.pageable.default-page-size=10",
				"spring.data.web.pageable.prefix=abc",
				"spring.data.web.pageable.qualifier-delimiter=__",
				"spring.data.web.pageable.max-page-size=100",
				"spring.data.web.pageable.one-indexed-parameters=true");
		PageableHandlerMethodArgumentResolver argumentResolver = this.context
				.getBean(PageableHandlerMethodArgumentResolver.class);
		assertThat(ReflectionTestUtils.getField(argumentResolver, "pageParameterName"))
				.isEqualTo("p");
		assertThat(ReflectionTestUtils.getField(argumentResolver, "sizeParameterName"))
				.isEqualTo("s");
		assertThat(ReflectionTestUtils.getField(argumentResolver, "oneIndexedParameters"))
				.isEqualTo(true);
		assertThat(ReflectionTestUtils.getField(argumentResolver, "prefix"))
				.isEqualTo("abc");
		assertThat(ReflectionTestUtils.getField(argumentResolver, "qualifierDelimiter"))
				.isEqualTo("__");
		assertThat(ReflectionTestUtils.getField(argumentResolver, "fallbackPageable"))
				.isEqualTo(PageRequest.of(0, 10));
		assertThat(ReflectionTestUtils.getField(argumentResolver, "maxPageSize"))
				.isEqualTo(100);
	}

	@Test
	public void defaultPageable() {
		load();
		PageableHandlerMethodArgumentResolver argumentResolver = this.context
				.getBean(PageableHandlerMethodArgumentResolver.class);
		SpringDataWebProperties.Pageable properties = new SpringDataWebProperties()
				.getPageable();
		assertThat(ReflectionTestUtils.getField(argumentResolver, "pageParameterName"))
				.isEqualTo(properties.getPageParameter());
		assertThat(ReflectionTestUtils.getField(argumentResolver, "sizeParameterName"))
				.isEqualTo(properties.getSizeParameter());
		assertThat(ReflectionTestUtils.getField(argumentResolver, "oneIndexedParameters"))
				.isEqualTo(properties.isOneIndexedParameters());
		assertThat(ReflectionTestUtils.getField(argumentResolver, "prefix"))
				.isEqualTo(properties.getPrefix());
		assertThat(ReflectionTestUtils.getField(argumentResolver, "qualifierDelimiter"))
				.isEqualTo(properties.getQualifierDelimiter());
		assertThat(ReflectionTestUtils.getField(argumentResolver, "fallbackPageable"))
				.isEqualTo(PageRequest.of(0, properties.getDefaultPageSize()));
		assertThat(ReflectionTestUtils.getField(argumentResolver, "maxPageSize"))
				.isEqualTo(properties.getMaxPageSize());
	}

	@Test
	public void customizeSort() {
		load("spring.data.web.sort.sort-parameter=s");
		SortHandlerMethodArgumentResolver argumentResolver = this.context
				.getBean(SortHandlerMethodArgumentResolver.class);
		assertThat(ReflectionTestUtils.getField(argumentResolver, "sortParameter"))
				.isEqualTo("s");
	}

	private void load(String... environment) {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(SpringDataWebAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

}
