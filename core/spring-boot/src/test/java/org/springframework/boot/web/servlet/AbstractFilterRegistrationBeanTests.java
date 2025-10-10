/*
 * Copyright 2012-present the original author or authors.
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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.filter.OncePerRequestFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Abstract base for {@link AbstractFilterRegistrationBean} tests.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractFilterRegistrationBeanTests {

	@Mock
	@SuppressWarnings("NullAway.Init")
	ServletContext servletContext;

	@Mock
	@SuppressWarnings("NullAway.Init")
	FilterRegistration.Dynamic registration;

	@Test
	void startupWithDefaults() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addFilter(eq("mockFilter"), getExpectedFilter());
		then(this.registration).should().setAsyncSupported(true);
		then(this.registration).should().addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
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
		then(this.servletContext).should().addFilter(eq("test"), getExpectedFilter());
		then(this.registration).should().setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		then(this.registration).should().setInitParameters(expectedInitParameters);
		then(this.registration).should()
			.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/a", "/b", "/c");
		then(this.registration).should()
			.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, "s4", "s5", "s1", "s2", "s3");
	}

	@Test
	void specificName() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setName("specificName");
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addFilter(eq("specificName"), getExpectedFilter());
	}

	@Test
	void deducedName() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addFilter(eq("mockFilter"), getExpectedFilter());
	}

	@Test
	void disable() throws Exception {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should(never()).addFilter(eq("mockFilter"), getExpectedFilter());
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void setServletRegistrationBeanMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setServletRegistrationBeans(null))
			.withMessageContaining("servletRegistrationBeans' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addServletRegistrationBeanMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> bean.addServletRegistrationBeans((ServletRegistrationBean[]) null))
			.withMessageContaining("'servletRegistrationBeans' must not be null");
	}

	@Test
	void setServletRegistrationBeanReplacesValue() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean(mockServletRegistration("a"));
		bean.setServletRegistrationBeans(new LinkedHashSet<>(Collections.singletonList(mockServletRegistration("b"))));
		bean.onStartup(this.servletContext);
		then(this.registration).should().addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, "b");
	}

	@Test
	void modifyInitParameters() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		then(this.registration).should().setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void setUrlPatternMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setUrlPatterns(null))
			.withMessageContaining("'urlPatterns' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addUrlPatternMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addUrlPatterns((String[]) null))
			.withMessageContaining("'urlPatterns' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void setServletNameMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setServletNames(null))
			.withMessageContaining("'servletNames' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addServletNameMustNotBeNull() {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addServletNames((String[]) null))
			.withMessageContaining("'servletNames' must not be null");
	}

	@Test
	void withSpecificDispatcherTypes() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setDispatcherTypes(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		bean.onStartup(this.servletContext);
		then(this.registration).should()
			.addMappingForUrlPatterns(EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD), false, "/*");
	}

	@Test
	void withSpecificDispatcherTypesEnumSet() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		bean.setDispatcherTypes(types);
		bean.onStartup(this.servletContext);
		then(this.registration).should().addMappingForUrlPatterns(types, false, "/*");
	}

	@Test
	void failsWithDoubleRegistration() {
		assertThatIllegalStateException().isThrownBy(this::doubleRegistration)
			.withMessage("Failed to register 'filter double-registration' on the "
					+ "servlet context. Possibly already registered?");
	}

	private void doubleRegistration() throws ServletException {
		AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
		bean.setName("double-registration");
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(null);
		bean.onStartup(this.servletContext);
	}

	@Test
	void doesntFailIfDoubleRegistrationIsIgnored() {
		assertThatCode(() -> {
			AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
			bean.setName("double-registration");
			given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(null);
			bean.setIgnoreRegistrationFailure(true);
			bean.onStartup(this.servletContext);
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldDetermineDispatcherTypesIfNotSet() {
		AbstractFilterRegistrationBean<SimpleFilter> simpleFilter = new AbstractFilterRegistrationBean<>() {
			@Override
			public SimpleFilter getFilter() {
				return new SimpleFilter();
			}
		};
		assertThat(simpleFilter.determineDispatcherTypes()).containsExactly(DispatcherType.REQUEST);
	}

	@Test
	void shouldDetermineDispatcherTypesForOncePerRequestFilters() {
		AbstractFilterRegistrationBean<SimpleOncePerRequestFilter> simpleFilter = new AbstractFilterRegistrationBean<>() {
			@Override
			public SimpleOncePerRequestFilter getFilter() {
				return new SimpleOncePerRequestFilter();
			}
		};
		assertThat(simpleFilter.determineDispatcherTypes())
			.containsExactlyInAnyOrderElementsOf(EnumSet.allOf(DispatcherType.class));
	}

	@Test
	void shouldDetermineDispatcherTypesForSetDispatcherTypes() {
		AbstractFilterRegistrationBean<SimpleFilter> simpleFilter = new AbstractFilterRegistrationBean<>() {
			@Override
			public SimpleFilter getFilter() {
				return new SimpleFilter();
			}
		};
		simpleFilter.setDispatcherTypes(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		assertThat(simpleFilter.determineDispatcherTypes()).containsExactlyInAnyOrder(DispatcherType.INCLUDE,
				DispatcherType.FORWARD);
	}

	protected abstract Filter getExpectedFilter();

	protected abstract AbstractFilterRegistrationBean<?> createFilterRegistrationBean(
			ServletRegistrationBean<?>... servletRegistrationBeans);

	protected final ServletRegistrationBean<?> mockServletRegistration(String name) {
		ServletRegistrationBean<?> bean = new ServletRegistrationBean<>();
		bean.setName(name);
		return bean;
	}

	private static final class SimpleFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

		}

	}

	private static final class SimpleOncePerRequestFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) {

		}

	}

}
