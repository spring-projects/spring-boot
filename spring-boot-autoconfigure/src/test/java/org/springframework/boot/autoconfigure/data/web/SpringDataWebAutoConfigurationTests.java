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

package org.springframework.boot.autoconfigure.data.web;

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
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
 */
public class SpringDataWebAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void webSupportIsAutoConfiguredInWebApplicationContexts() {
		this.context = new AnnotationConfigWebApplicationContext();
		((AnnotationConfigWebApplicationContext) this.context)
				.register(SpringDataWebAutoConfiguration.class);
		this.context.refresh();
		((AnnotationConfigWebApplicationContext) this.context)
				.setServletContext(new MockServletContext());
		Map<String, PageableHandlerMethodArgumentResolver> beans = this.context
				.getBeansOfType(PageableHandlerMethodArgumentResolver.class);
		assertThat(beans).hasSize(1);
	}

	@Test
	public void autoConfigurationBacksOffInNonWebApplicationContexts() {
		this.context = new AnnotationConfigApplicationContext();
		((AnnotationConfigApplicationContext) this.context)
				.register(SpringDataWebAutoConfiguration.class);
		this.context.refresh();
		Map<String, PageableHandlerMethodArgumentResolver> beans = this.context
				.getBeansOfType(PageableHandlerMethodArgumentResolver.class);
		assertThat(beans).isEmpty();
	}

	@Test
	public void pageable() {
		this.context = new AnnotationConfigWebApplicationContext();
		TestPropertyValues
				.of("spring.data.web.pageable.page-parameter=p",
						"spring.data.web.pageable.size-parameter=s",
						"spring.data.web.pageable.default-page-size=10")
				.applyTo(this.context);
		((AnnotationConfigWebApplicationContext) this.context)
				.register(SpringDataWebAutoConfiguration.class);
		this.context.refresh();
		PageableHandlerMethodArgumentResolver argumentResolver = this.context
				.getBean(PageableHandlerMethodArgumentResolver.class);
		assertThat(ReflectionTestUtils.getField(argumentResolver, "pageParameterName"))
				.isEqualTo("p");
		assertThat(ReflectionTestUtils.getField(argumentResolver, "sizeParameterName"))
				.isEqualTo("s");
		assertThat(ReflectionTestUtils.getField(argumentResolver, "fallbackPageable"))
				.isEqualTo(PageRequest.of(0, 10));
	}

	@Test
	public void sort() {
		this.context = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of("spring.data.web.sort.sort-parameter=s")
				.applyTo(this.context);
		((AnnotationConfigWebApplicationContext) this.context)
				.register(SpringDataWebAutoConfiguration.class);
		this.context.refresh();
		SortHandlerMethodArgumentResolver argumentResolver = this.context
				.getBean(SortHandlerMethodArgumentResolver.class);
		assertThat(ReflectionTestUtils.getField(argumentResolver, "sortParameter"))
				.isEqualTo("s");
	}

}
