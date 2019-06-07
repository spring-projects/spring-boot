/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFilterHandler}
 *
 * @author Andy Wilkinson
 */
public class WebFilterHandlerTests {

	private final WebFilterHandler handler = new WebFilterHandler();

	private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@SuppressWarnings("unchecked")
	@Test
	public void defaultFilterConfiguration() throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory().getMetadataReader(DefaultConfigurationFilter.class.getName()));
		this.handler.handle(scanned, this.registry);
		BeanDefinition filterRegistrationBean = this.registry
				.getBeanDefinition(DefaultConfigurationFilter.class.getName());
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("asyncSupported")).isEqualTo(false);
		assertThat((EnumSet<DispatcherType>) propertyValues.get("dispatcherTypes"))
				.containsExactly(DispatcherType.REQUEST);
		assertThat(((Map<String, String>) propertyValues.get("initParameters"))).isEmpty();
		assertThat((String[]) propertyValues.get("servletNames")).isEmpty();
		assertThat((String[]) propertyValues.get("urlPatterns")).isEmpty();
		assertThat(propertyValues.get("name")).isEqualTo(DefaultConfigurationFilter.class.getName());
		assertThat(propertyValues.get("filter")).isEqualTo(scanned);
	}

	@Test
	public void filterWithCustomName() throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory().getMetadataReader(CustomNameFilter.class.getName()));
		this.handler.handle(scanned, this.registry);
		BeanDefinition filterRegistrationBean = this.registry.getBeanDefinition("custom");
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("name")).isEqualTo("custom");
	}

	@Test
	public void asyncSupported() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(AsyncSupportedFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("asyncSupported")).isEqualTo(true);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void dispatcherTypes() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(DispatcherTypesFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((Set<DispatcherType>) propertyValues.get("dispatcherTypes")).containsExactly(DispatcherType.FORWARD,
				DispatcherType.INCLUDE, DispatcherType.REQUEST);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void initParameters() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(InitParametersFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((Map<String, String>) propertyValues.get("initParameters")).containsEntry("a", "alpha")
				.containsEntry("b", "bravo");
	}

	@Test
	public void servletNames() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(ServletNamesFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("servletNames")).contains("alpha", "bravo");
	}

	@Test
	public void urlPatterns() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(UrlPatternsFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("urlPatterns")).contains("alpha", "bravo");
	}

	@Test
	public void urlPatternsFromValue() throws IOException {
		BeanDefinition filterRegistrationBean = getBeanDefinition(UrlPatternsFromValueFilter.class);
		MutablePropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("urlPatterns")).contains("alpha", "bravo");
	}

	@Test
	public void urlPatternsDeclaredTwice() throws IOException {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("The urlPatterns and value attributes are mutually exclusive.");
		getBeanDefinition(UrlPatternsDeclaredTwiceFilter.class);
	}

	BeanDefinition getBeanDefinition(Class<?> filterClass) throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory().getMetadataReader(filterClass.getName()));
		this.handler.handle(scanned, this.registry);
		return this.registry.getBeanDefinition(filterClass.getName());
	}

	@WebFilter
	class DefaultConfigurationFilter extends BaseFilter {

	}

	@WebFilter(asyncSupported = true)
	class AsyncSupportedFilter extends BaseFilter {

	}

	@WebFilter(dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE })
	class DispatcherTypesFilter extends BaseFilter {

	}

	@WebFilter(initParams = { @WebInitParam(name = "a", value = "alpha"), @WebInitParam(name = "b", value = "bravo") })
	class InitParametersFilter extends BaseFilter {

	}

	@WebFilter(servletNames = { "alpha", "bravo" })
	class ServletNamesFilter extends BaseFilter {

	}

	@WebFilter(urlPatterns = { "alpha", "bravo" })
	class UrlPatternsFilter extends BaseFilter {

	}

	@WebFilter({ "alpha", "bravo" })
	class UrlPatternsFromValueFilter extends BaseFilter {

	}

	@WebFilter(value = { "alpha", "bravo" }, urlPatterns = { "alpha", "bravo" })
	class UrlPatternsDeclaredTwiceFilter extends BaseFilter {

	}

	@WebFilter(filterName = "custom")
	class CustomNameFilter extends BaseFilter {

	}

	class BaseFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		}

		@Override
		public void destroy() {

		}

	}

}
