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

package org.springframework.boot.autoconfigure.freemarker;

import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
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
public class FreeMarkerAutoConfigurationServletIntegrationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		load();
		assertThat(this.context.getBean(FreeMarkerViewResolver.class)).isNotNull();
		assertThat(this.context.getBean(FreeMarkerConfigurer.class)).isNotNull();
		assertThat(this.context.getBean(FreeMarkerConfig.class)).isNotNull();
		assertThat(this.context.getBean(freemarker.template.Configuration.class)).isNotNull();
	}

	@Test
	public void defaultViewResolution() throws Exception {
		load();
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	public void customContentType() throws Exception {
		load("spring.freemarker.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
	}

	@Test
	public void customPrefix() throws Exception {
		load("spring.freemarker.prefix:prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result).contains("prefixed");
	}

	@Test
	public void customSuffix() throws Exception {
		load("spring.freemarker.suffix:.freemarker");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result).contains("suffixed");
	}

	@Test
	public void customTemplateLoaderPath() throws Exception {
		load("spring.freemarker.templateLoaderPath:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result).contains("custom");
	}

	@Test
	public void disableCache() {
		load("spring.freemarker.cache:false");
		assertThat(this.context.getBean(FreeMarkerViewResolver.class).getCacheLimit()).isEqualTo(0);
	}

	@Test
	public void allowSessionOverride() {
		load("spring.freemarker.allow-session-override:true");
		AbstractTemplateViewResolver viewResolver = this.context.getBean(FreeMarkerViewResolver.class);
		assertThat(ReflectionTestUtils.getField(viewResolver, "allowSessionOverride")).isEqualTo(true);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void customFreeMarkerSettings() {
		load("spring.freemarker.settings.boolean_format:yup,nope");
		assertThat(this.context.getBean(FreeMarkerConfigurer.class).getConfiguration().getSetting("boolean_format"))
				.isEqualTo("yup,nope");
	}

	@Test
	public void renderTemplate() throws Exception {
		load();
		FreeMarkerConfigurer freemarker = this.context.getBean(FreeMarkerConfigurer.class);
		StringWriter writer = new StringWriter();
		freemarker.getConfiguration().getTemplate("message.ftl").process(this, writer);
		assertThat(writer.toString()).contains("Hello World");
	}

	@Test
	public void registerResourceHandlingFilterDisabledByDefault() {
		load();
		assertThat(this.context.getBeansOfType(FilterRegistrationBean.class)).isEmpty();
	}

	@Test
	public void registerResourceHandlingFilterOnlyIfResourceChainIsEnabled() {
		load("spring.resources.chain.enabled:true");
		FilterRegistrationBean<?> registration = this.context.getBean(FilterRegistrationBean.class);
		assertThat(registration.getFilter()).isInstanceOf(ResourceUrlEncodingFilter.class);
		assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void registerResourceHandlingFilterWithOtherRegistrationBean() {
		// gh-14897
		load(FilterRegistrationConfiguration.class, "spring.resources.chain.enabled:true");
		Map<String, FilterRegistrationBean> beans = this.context.getBeansOfType(FilterRegistrationBean.class);
		assertThat(beans).hasSize(2);
		FilterRegistrationBean registration = beans.values().stream()
				.filter((r) -> r.getFilter() instanceof ResourceUrlEncodingFilter).findFirst().get();
		assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
	}

	private void load(String... env) {
		load(BaseConfiguration.class, env);
	}

	private void load(Class<?> config, String... env) {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		TestPropertyValues.of(env).applyTo(this.context);
		this.context.register(config);
		this.context.refresh();
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		FreeMarkerViewResolver resolver = this.context.getBean(FreeMarkerViewResolver.class);
		View view = resolver.resolveViewName(viewName, Locale.UK);
		assertThat(view).isNotNull();
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}

	@Configuration
	@ImportAutoConfiguration({ FreeMarkerAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class BaseConfiguration {

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class FilterRegistrationConfiguration {

		@Bean
		public FilterRegistrationBean<OrderedCharacterEncodingFilter> filterRegisration() {
			return new FilterRegistrationBean<OrderedCharacterEncodingFilter>(new OrderedCharacterEncodingFilter());
		}

	}

}
