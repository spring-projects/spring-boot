/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Abstract base for {@link AbstractFilterRegistrationBean} tests.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractFilterRegistrationBeanTests {

	@Mock
	ServletContext servletContext;

	@Mock
	FilterRegistration.Dynamic registration;

	@Test
	void startupWithDefaults() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("mockFilter"), getExpectedFilter());
		verify(this.registration).setAsyncSupported(true);
		verify(this.registration).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
	}

	@Test
	void startupWithSpecifiedValues() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setName("test");
		bean.setAsyncSupported(false);
		bean.setInitParameters(Collections.singletonMap("a", "b"));
		bean.addInitParameter("c", "d");
		bean.setUrlPatterns(new LinkedHashSet<>(Arrays.asList("/a", "/b")));
		bean.addUrlPatterns("/c");
		bean.setServletNames(new LinkedHashSet<>(Arrays.asList("s1", "s2")));
		bean.addServletNames("s3");
		bean.setServletRegistrationBeans(Collections.singleton(mockServletRegistration("s4")));
		bean.addServletRegistrationBeans(mockServletRegistration("s5"));
		bean.setMatchAfter(true);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("test"), getExpectedFilter());
		verify(this.registration).setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		verify(this.registration).setInitParameters(expectedInitParameters);
		verify(this.registration).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/a", "/b", "/c");
		verify(this.registration).addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, "s4", "s5", "s1",
				"s2", "s3");
	}

	@Test
	void specificName() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setName("specificName");
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("specificName"), getExpectedFilter());
	}

	@Test
	void deducedName() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("mockFilter"), getExpectedFilter());
	}

	@Test
	void disable() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		verify(this.servletContext, never()).addFilter(eq("mockFilter"), getExpectedFilter());
	}

	@Test
	void setServletRegistrationBeanMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setServletRegistrationBeans(null))
				.withMessageContaining("ServletRegistrationBeans must not be null");
	}

	@Test
	void addServletRegistrationBeanMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> bean.addServletRegistrationBeans((ServletRegistrationBean[]) null))
				.withMessageContaining("ServletRegistrationBeans must not be null");
	}

	@Test
	void setServletRegistrationBeanReplacesValue() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean(mockServletRegistration("a"));
		bean.setServletRegistrationBeans(
				new LinkedHashSet<ServletRegistrationBean<?>>(Collections.singletonList(mockServletRegistration("b"))));
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, "b");
	}

	@Test
	void modifyInitParameters() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		verify(this.registration).setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	void setUrlPatternMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setUrlPatterns(null))
				.withMessageContaining("UrlPatterns must not be null");
	}

	@Test
	void addUrlPatternMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addUrlPatterns((String[]) null))
				.withMessageContaining("UrlPatterns must not be null");
	}

	@Test
	void setServletNameMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setServletNames(null))
				.withMessageContaining("ServletNames must not be null");
	}

	@Test
	void addServletNameMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addServletNames((String[]) null))
				.withMessageContaining("ServletNames must not be null");
	}

	@Test
	void withSpecificDispatcherTypes() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setDispatcherTypes(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForUrlPatterns(EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD),
				false, "/*");
	}

	@Test
	void withSpecificDispatcherTypesEnumSet() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		bean.setDispatcherTypes(types);
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForUrlPatterns(types, false, "/*");
	}

	protected abstract Filter getExpectedFilter();

	protected abstract AbstractFilterRegistrationBean<?> createFilterRegistrationBean(
			ServletRegistrationBean<?>... servletRegistrationBeans);

	protected final ServletRegistrationBean<?> mockServletRegistration(String name) {
		ServletRegistrationBean<?> bean = new ServletRegistrationBean<>();
		bean.setName(name);
		return bean;
	}

}
