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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Abstract base for {@link AbstractFilterRegistrationBean} tests.
 *
 * @author Phillip Webb
 */
public abstract class AbstractFilterRegistrationBeanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	ServletContext servletContext;

	@Mock
	FilterRegistration.Dynamic registration;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
		given(this.servletContext.addFilter(anyString(), any(Filter.class)))
				.willReturn(this.registration);
	}

	@Test
	public void startupWithDefaults() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("mockFilter"), getExpectedFilter());
		verify(this.registration).setAsyncSupported(true);
		verify(this.registration).addMappingForUrlPatterns(
				EnumSet.of(DispatcherType.REQUEST), false, "/*");
	}

	@Test
	public void startupWithSpecifiedValues() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setName("test");
		bean.setAsyncSupported(false);
		bean.setInitParameters(Collections.singletonMap("a", "b"));
		bean.addInitParameter("c", "d");
		bean.setUrlPatterns(new LinkedHashSet<>(Arrays.asList("/a", "/b")));
		bean.addUrlPatterns("/c");
		bean.setServletNames(new LinkedHashSet<>(Arrays.asList("s1", "s2")));
		bean.addServletNames("s3");
		bean.setServletRegistrationBeans(
				Collections.singleton(mockServletRegistration("s4")));
		bean.addServletRegistrationBeans(mockServletRegistration("s5"));
		bean.setMatchAfter(true);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("test"), getExpectedFilter());
		verify(this.registration).setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		verify(this.registration).setInitParameters(expectedInitParameters);
		verify(this.registration).addMappingForUrlPatterns(
				EnumSet.of(DispatcherType.REQUEST), true, "/a", "/b", "/c");
		verify(this.registration).addMappingForServletNames(
				EnumSet.of(DispatcherType.REQUEST), true, "s4", "s5", "s1", "s2", "s3");
	}

	@Test
	public void specificName() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setName("specificName");
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("specificName"), getExpectedFilter());
	}

	@Test
	public void deducedName() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter(eq("mockFilter"), getExpectedFilter());
	}

	@Test
	public void disable() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		verify(this.servletContext, times(0)).addFilter(eq("mockFilter"),
				getExpectedFilter());
	}

	@Test
	public void setServletRegistrationBeanMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		bean.setServletRegistrationBeans(null);
	}

	@Test
	public void addServletRegistrationBeanMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		bean.addServletRegistrationBeans((ServletRegistrationBean[]) null);
	}

	@Test
	public void setServletRegistrationBeanReplacesValue() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean(
				mockServletRegistration("a"));
		bean.setServletRegistrationBeans(new LinkedHashSet<ServletRegistrationBean<?>>(
				Arrays.asList(mockServletRegistration("b"))));
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForServletNames(
				EnumSet.of(DispatcherType.REQUEST), false, "b");
	}

	@Test
	public void modifyInitParameters() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		verify(this.registration).setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	public void setUrlPatternMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("UrlPatterns must not be null");
		bean.setUrlPatterns(null);
	}

	@Test
	public void addUrlPatternMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("UrlPatterns must not be null");
		bean.addUrlPatterns((String[]) null);
	}

	@Test
	public void setServletNameMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletNames must not be null");
		bean.setServletNames(null);
	}

	@Test
	public void addServletNameMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletNames must not be null");
		bean.addServletNames((String[]) null);
	}

	@Test
	public void withSpecificDispatcherTypes() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setDispatcherTypes(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForUrlPatterns(
				EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD), false, "/*");
	}

	@Test
	public void withSpecificDispatcherTypesEnumSet() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.INCLUDE,
				DispatcherType.FORWARD);
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
