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

package org.springframework.boot.web.servlet;

import javax.servlet.Filter;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.servlet.mock.MockFilter;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for {@link FilterRegistrationBean} when {@code spring-web} is not on the
 * classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-web-*.jar")
class NoSpringWebFilterRegistrationBeanTests extends AbstractFilterRegistrationBeanTests {

	private final MockFilter filter = new MockFilter();

	@Override
	protected AbstractFilterRegistrationBean<MockFilter> createFilterRegistrationBean(
			ServletRegistrationBean<?>... servletRegistrationBeans) {
		return new FilterRegistrationBean<>(this.filter, servletRegistrationBeans);
	}

	@Override
	protected Filter getExpectedFilter() {
		return eq(this.filter);
	}

}
