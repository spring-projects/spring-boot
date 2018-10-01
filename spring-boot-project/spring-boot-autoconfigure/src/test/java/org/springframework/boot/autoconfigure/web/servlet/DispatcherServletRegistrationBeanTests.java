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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Collections;

import org.junit.Test;

import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DispatcherServletRegistrationBean}.
 *
 * @author Phillip Webb
 */
public class DispatcherServletRegistrationBeanTests {

	@Test
	public void createWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DispatcherServletRegistrationBean(
						new DispatcherServlet(), null))
				.withMessageContaining("Path must not be null");
	}

	@Test
	public void getPathReturnsPath() {
		DispatcherServletRegistrationBean bean = new DispatcherServletRegistrationBean(
				new DispatcherServlet(), "/test");
		assertThat(bean.getPath()).isEqualTo("/test");
	}

	@Test
	public void getUrlMappingsReturnsSinglePathMappedPattern() {
		DispatcherServletRegistrationBean bean = new DispatcherServletRegistrationBean(
				new DispatcherServlet(), "/test");
		assertThat(bean.getUrlMappings()).containsOnly("/test/*");
	}

	@Test
	public void setUrlMappingsCannotBeCalled() {
		DispatcherServletRegistrationBean bean = new DispatcherServletRegistrationBean(
				new DispatcherServlet(), "/test");
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> bean.setUrlMappings(Collections.emptyList()));
	}

	@Test
	public void addUrlMappingsCannotBeCalled() {
		DispatcherServletRegistrationBean bean = new DispatcherServletRegistrationBean(
				new DispatcherServlet(), "/test");
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> bean.addUrlMappings("/test"));
	}

}
