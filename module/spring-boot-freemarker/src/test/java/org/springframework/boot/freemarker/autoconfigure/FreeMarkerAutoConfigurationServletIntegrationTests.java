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

package org.springframework.boot.freemarker.autoconfigure;

import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration} Servlet support.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
class FreeMarkerAutoConfigurationServletIntegrationTests {

	private @Nullable AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void defaultConfiguration() {
		load();
		assertThat(getContext().getBean(FreeMarkerViewResolver.class)).isNotNull();
		assertThat(getContext().getBean(FreeMarkerConfigurer.class)).isNotNull();
		assertThat(getContext().getBean(FreeMarkerConfig.class)).isNotNull();
		assertThat(getContext().getBean(freemarker.template.Configuration.class)).isNotNull();
	}

	@Test
	@WithResource(name = "templates/home.ftlh", content = "home")
	void defaultViewResolution() throws Exception {
		load();
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	@WithResource(name = "templates/home.ftlh", content = "home")
	void customContentType() throws Exception {
		load("spring.freemarker.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
	}

	@Test
	@WithResource(name = "templates/prefix/prefixed.ftlh", content = "prefixed")
	void customPrefix() throws Exception {
		load("spring.freemarker.prefix:prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result).contains("prefixed");
	}

	@Test
	@WithResource(name = "templates/suffixed.freemarker", content = "suffixed")
	void customSuffix() throws Exception {
		load("spring.freemarker.suffix:.freemarker");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result).contains("suffixed");
	}

	@Test
	@WithResource(name = "custom-templates/custom.ftlh", content = "custom")
	void customTemplateLoaderPath() throws Exception {
		load("spring.freemarker.templateLoaderPath:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result).contains("custom");
	}

	@Test
	void disableCache() {
		load("spring.freemarker.cache:false");
		assertThat(getContext().getBean(FreeMarkerViewResolver.class).getCacheLimit()).isZero();
	}

	@Test
	void allowSessionOverride() {
		load("spring.freemarker.allow-session-override:true");
		AbstractTemplateViewResolver viewResolver = getContext().getBean(FreeMarkerViewResolver.class);
		assertThat(viewResolver).hasFieldOrPropertyWithValue("allowSessionOverride", true);
	}

	@SuppressWarnings("deprecation")
	@Test
	void customFreeMarkerSettings() {
		load("spring.freemarker.settings.boolean_format:yup,nope");
		assertThat(getContext().getBean(FreeMarkerConfigurer.class).getConfiguration().getSetting("boolean_format"))
			.isEqualTo("yup,nope");
	}

	@Test
	@WithResource(name = "templates/message.ftlh", content = "Message: ${greeting}")
	void renderTemplate() throws Exception {
		load();
		FreeMarkerConfigurer freemarker = getContext().getBean(FreeMarkerConfigurer.class);
		StringWriter writer = new StringWriter();
		freemarker.getConfiguration().getTemplate("message.ftlh").process(new DataModel(), writer);
		assertThat(writer.toString()).contains("Hello World");
	}

	@Test
	void registerResourceHandlingFilterDisabledByDefault() {
		load();
		assertThat(getContext().getBeansOfType(FilterRegistrationBean.class)).isEmpty();
	}

	@Test
	void registerResourceHandlingFilterOnlyIfResourceChainIsEnabled() {
		load("spring.web.resources.chain.enabled:true");
		FilterRegistrationBean<?> registration = getContext().getBean(FilterRegistrationBean.class);
		assertThat(registration.getFilter()).isInstanceOf(ResourceUrlEncodingFilter.class);
		assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
	}

	@Test
	@SuppressWarnings("rawtypes")
	void registerResourceHandlingFilterWithOtherRegistrationBean() {
		// gh-14897
		load(FilterRegistrationOtherConfiguration.class, "spring.web.resources.chain.enabled:true");
		Map<String, FilterRegistrationBean> beans = getContext().getBeansOfType(FilterRegistrationBean.class);
		assertThat(beans).hasSize(2);
		FilterRegistrationBean registration = beans.values()
			.stream()
			.filter((r) -> r.getFilter() instanceof ResourceUrlEncodingFilter)
			.findFirst()
			.get();
		assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
	}

	@Test
	@SuppressWarnings("rawtypes")
	void registerResourceHandlingFilterWithResourceRegistrationBean() {
		// gh-14926
		load(FilterRegistrationResourceConfiguration.class, "spring.web.resources.chain.enabled:true");
		Map<String, FilterRegistrationBean> beans = getContext().getBeansOfType(FilterRegistrationBean.class);
		assertThat(beans).hasSize(1);
		FilterRegistrationBean registration = beans.values()
			.stream()
			.filter((r) -> r.getFilter() instanceof ResourceUrlEncodingFilter)
			.findFirst()
			.get();
		assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes", EnumSet.of(DispatcherType.INCLUDE));
	}

	private void load(String... env) {
		load(BaseConfiguration.class, env);
	}

	private void load(Class<?> config, String... env) {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		TestPropertyValues.of(env).applyTo(this.context);
		this.context.register(config);
		this.context.refresh();
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		FreeMarkerViewResolver resolver = getContext().getBean(FreeMarkerViewResolver.class);
		View view = resolver.resolveViewName(viewName, Locale.UK);
		assertThat(view).isNotNull();
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}

	private AnnotationConfigServletWebApplicationContext getContext() {
		AnnotationConfigServletWebApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ FreeMarkerAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class BaseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class FilterRegistrationResourceConfiguration {

		@Bean
		FilterRegistrationBean<ResourceUrlEncodingFilter> filterRegistration() {
			FilterRegistrationBean<ResourceUrlEncodingFilter> bean = new FilterRegistrationBean<>(
					new ResourceUrlEncodingFilter());
			bean.setDispatcherTypes(EnumSet.of(DispatcherType.INCLUDE));
			return bean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class FilterRegistrationOtherConfiguration {

		@Bean
		FilterRegistrationBean<OrderedCharacterEncodingFilter> filterRegistration() {
			return new FilterRegistrationBean<>(new OrderedCharacterEncodingFilter());
		}

	}

	public static class DataModel {

		public String getGreeting() {
			return "Hello World";
		}

	}

}
