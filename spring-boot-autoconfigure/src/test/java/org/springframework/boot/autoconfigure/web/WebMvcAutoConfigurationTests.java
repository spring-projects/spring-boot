/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link WebMvcAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class WebMvcAutoConfigurationTests {

	private static final MockEmbeddedServletContainerFactory containerFactory = new MockEmbeddedServletContainerFactory();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void handerAdaptersCreated() throws Exception {
		load();
		assertEquals(3, this.context.getBeanNamesForType(HandlerAdapter.class).length);
		assertFalse(this.context.getBean(RequestMappingHandlerAdapter.class)
				.getMessageConverters().isEmpty());
		assertEquals(this.context.getBean(HttpMessageConverters.class).getConverters(),
				this.context.getBean(RequestMappingHandlerAdapter.class)
						.getMessageConverters());
	}

	@Test
	public void handerMappingsCreated() throws Exception {
		load();
		assertEquals(6, this.context.getBeanNamesForType(HandlerMapping.class).length);
	}

	@Test
	public void resourceHandlerMapping() throws Exception {
		load();
		Map<String, List<Resource>> mappingLocations = getResourceMappingLocations();
		assertThat(mappingLocations.get("/**").size(), equalTo(5));
		assertThat(mappingLocations.get("/webjars/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/webjars/**").get(0),
				equalTo((Resource) new ClassPathResource("/META-INF/resources/webjars/")));
	}

	@Test
	public void resourceHandlerMappingOverrideWebjars() throws Exception {
		load(WebJars.class);
		Map<String, List<Resource>> mappingLocations = getResourceMappingLocations();
		assertThat(mappingLocations.get("/webjars/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/webjars/**").get(0),
				equalTo((Resource) new ClassPathResource("/foo/")));
	}

	@Test
	public void resourceHandlerMappingOverrideAll() throws Exception {
		load(AllResources.class);
		Map<String, List<Resource>> mappingLocations = getResourceMappingLocations();
		assertThat(mappingLocations.get("/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/**").get(0),
				equalTo((Resource) new ClassPathResource("/foo/")));
	}

	@Test
	public void resourceHandlerMappingDisabled() throws Exception {
		load("spring.resources.add-mappings:false");
		Map<String, List<Resource>> mappingLocations = getResourceMappingLocations();
		assertThat(mappingLocations.size(), equalTo(0));
	}

	@Test
	public void noLocaleResolver() throws Exception {
		load(AllResources.class);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(LocaleResolver.class);
	}

	@Test
	public void overrideLocale() throws Exception {
		load(AllResources.class, "spring.mvc.locale:en_UK");

		// mock request and set user preferred locale
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
		LocaleResolver localeResolver = this.context.getBean(LocaleResolver.class);
		Locale locale = localeResolver.resolveLocale(request);
		assertThat(localeResolver, instanceOf(FixedLocaleResolver.class));
		// test locale resolver uses fixed locale and not user preferred locale
		assertThat(locale.toString(), equalTo("en_UK"));
	}

	@Test
	public void noDateFormat() throws Exception {
		load(AllResources.class);
		FormattingConversionService cs = this.context
				.getBean(FormattingConversionService.class);
		Date date = new DateTime(1988, 6, 25, 20, 30).toDate();
		// formatting cs should use simple toString()
		assertThat(cs.convert(date, String.class), equalTo(date.toString()));
	}

	@Test
	public void overrideDateFormat() throws Exception {
		load(AllResources.class, "spring.mvc.dateFormat:dd*MM*yyyy");
		FormattingConversionService cs = this.context
				.getBean(FormattingConversionService.class);
		Date date = new DateTime(1988, 6, 25, 20, 30).toDate();
		assertThat(cs.convert(date, String.class), equalTo("25*06*1988"));
	}

	@Test
	public void noMessageCodesResolver() throws Exception {
		load(AllResources.class);
		assertNull(this.context.getBean(WebMvcAutoConfigurationAdapter.class)
				.getMessageCodesResolver());
	}

	@Test
	public void overrideMessageCodesFormat() throws Exception {
		load(AllResources.class,
				"spring.mvc.messageCodesResolverFormat:POSTFIX_ERROR_CODE");
		assertNotNull(this.context.getBean(WebMvcAutoConfigurationAdapter.class)
				.getMessageCodesResolver());
	}

	protected Map<String, List<Resource>> getFaviconMappingLocations()
			throws IllegalAccessException {
		HandlerMapping mapping = (HandlerMapping) this.context
				.getBean("faviconHandlerMapping");
		return getMappingLocations(mapping);
	}

	protected Map<String, List<Resource>> getResourceMappingLocations()
			throws IllegalAccessException {
		HandlerMapping mapping = (HandlerMapping) this.context
				.getBean("resourceHandlerMapping");
		return getMappingLocations(mapping);
	}

	@SuppressWarnings("unchecked")
	protected Map<String, List<Resource>> getMappingLocations(HandlerMapping mapping)
			throws IllegalAccessException {
		Map<String, List<Resource>> mappingLocations = new LinkedHashMap<String, List<Resource>>();
		if (mapping instanceof SimpleUrlHandlerMapping) {
			Field locationsField = ReflectionUtils.findField(
					ResourceHttpRequestHandler.class, "locations");
			locationsField.setAccessible(true);
			for (Map.Entry<String, Object> entry : ((SimpleUrlHandlerMapping) mapping)
					.getHandlerMap().entrySet()) {
				ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) entry
						.getValue();
				mappingLocations.put(entry.getKey(),
						(List<Resource>) locationsField.get(handler));
			}
		}
		return mappingLocations;
	}

	@Test
	public void ignoreDefaultModelOnRedirectIsTrue() throws Exception {
		load();
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		assertEquals(true,
				ReflectionTestUtils.getField(adapter, "ignoreDefaultModelOnRedirect"));
	}

	@Test
	public void overrideIgnoreDefaultModelOnRedirect() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mvc.ignore-default-model-on-redirect:false");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		assertEquals(false,
				ReflectionTestUtils.getField(adapter, "ignoreDefaultModelOnRedirect"));
	}

	@Test
	public void customViewResolver() throws Exception {
		load(CustomViewResolver.class);
		assertThat(this.context.getBean("viewResolver"), instanceOf(MyViewResolver.class));
	}

	@Test
	public void customContentNegotiatingViewResolver() throws Exception {
		load(CustomContentNegotiatingViewResolver.class);
		Map<String, ContentNegotiatingViewResolver> beans = this.context
				.getBeansOfType(ContentNegotiatingViewResolver.class);
		assertThat(beans.size(), equalTo(1));
		assertThat(beans.keySet().iterator().next(), equalTo("myViewResolver"));
	}

	@Test
	public void faviconMapping() throws IllegalAccessException {
		load();
		assertThat(
				this.context.getBeansOfType(ResourceHttpRequestHandler.class).get(
						"faviconRequestHandler"), is(notNullValue()));
		assertThat(
				this.context.getBeansOfType(SimpleUrlHandlerMapping.class).get(
						"faviconHandlerMapping"), is(notNullValue()));
		Map<String, List<Resource>> mappingLocations = getFaviconMappingLocations();
		assertThat(mappingLocations.get("/**/favicon.ico").size(), equalTo(5));
	}

	@Test
	public void faviconMappingDisabled() throws IllegalAccessException {
		load("spring.mvc.favicon.enabled:false");
		assertThat(
				this.context.getBeansOfType(ResourceHttpRequestHandler.class).get(
						"faviconRequestHandler"), is(nullValue()));
		assertThat(
				this.context.getBeansOfType(SimpleUrlHandlerMapping.class).get(
						"faviconHandlerMapping"), is(nullValue()));
	}

	@Test
	public void defaultAsyncRequestTimeout() throws Exception {
		load();
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		assertNull(ReflectionTestUtils.getField(adapter, "asyncRequestTimeout"));
	}

	@Test
	public void customAsyncRequestTimeout() throws Exception {
		load("spring.mvc.async.request-timeout:123456");
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		Object actual = ReflectionTestUtils.getField(adapter, "asyncRequestTimeout");
		assertEquals(123456L, actual);
	}

	@SuppressWarnings("unchecked")
	private void load(Class<?> config, String... environment) {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		List<Class<?>> configClasses = new ArrayList<Class<?>>();
		if (config != null) {
			configClasses.add(config);
		}
		configClasses.addAll(Arrays.asList(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class));
		this.context.register(configClasses.toArray(new Class<?>[configClasses.size()]));
		this.context.refresh();
	}

	private void load(String... environment) {
		load(null, environment);
	}

	@Configuration
	protected static class ViewConfig {

		@Bean
		public View jsonView() {
			return new AbstractView() {

				@Override
				protected void renderMergedOutputModel(Map<String, Object> model,
						HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					response.getOutputStream().write("Hello World".getBytes());
				}
			};
		}

	}

	@Configuration
	protected static class WebJars extends WebMvcConfigurerAdapter {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/webjars/**").addResourceLocations(
					"classpath:/foo/");
		}

	}

	@Configuration
	protected static class AllResources extends WebMvcConfigurerAdapter {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/**").addResourceLocations("classpath:/foo/");
		}

	}

	@Configuration
	public static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	public static class CustomViewResolver {

		@Bean
		public ViewResolver viewResolver() {
			return new MyViewResolver();
		}

	}

	@Configuration
	public static class CustomContentNegotiatingViewResolver {

		@Bean
		public ContentNegotiatingViewResolver myViewResolver() {
			return new ContentNegotiatingViewResolver();
		}

	}

	private static class MyViewResolver implements ViewResolver {

		@Override
		public View resolveViewName(String viewName, Locale locale) throws Exception {
			return null;
		}

	}

}
