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

import org.hamcrest.Matcher;
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
import org.springframework.boot.context.web.OrderedHttpPutFormContentFilter;
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
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.filter.HttpPutFormContentFilter;
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
import org.springframework.web.servlet.resource.AppCacheManifestTransformer;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

import static org.hamcrest.Matchers.contains;
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
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link WebMvcAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Eddú Meléndez
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
	public void handlerAdaptersCreated() throws Exception {
		load();
		assertEquals(3, this.context.getBeanNamesForType(HandlerAdapter.class).length);
		assertFalse(this.context.getBean(RequestMappingHandlerAdapter.class)
				.getMessageConverters().isEmpty());
		assertEquals(this.context.getBean(HttpMessageConverters.class).getConverters(),
				this.context.getBean(RequestMappingHandlerAdapter.class)
						.getMessageConverters());
	}

	@Test
	public void handlerMappingsCreated() throws Exception {
		load();
		assertEquals(6, this.context.getBeanNamesForType(HandlerMapping.class).length);
	}

	@Test
	public void resourceHandlerMapping() throws Exception {
		load();
		Map<String, List<Resource>> mappingLocations = getResourceMappingLocations();
		assertThat(mappingLocations.get("/**").size(), equalTo(5));
		assertThat(mappingLocations.get("/webjars/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/webjars/**").get(0), equalTo(
				(Resource) new ClassPathResource("/META-INF/resources/webjars/")));
		assertThat(getResourceResolvers("/webjars/**").size(), equalTo(1));
		assertThat(getResourceTransformers("/webjars/**").size(), equalTo(0));
		assertThat(getResourceResolvers("/**").size(), equalTo(1));
		assertThat(getResourceTransformers("/**").size(), equalTo(0));
	}

	@Test
	public void customResourceHandlerMapping() throws Exception {
		load("spring.mvc.static-path-pattern:/static/**");
		Map<String, List<Resource>> mappingLocations = getResourceMappingLocations();
		assertThat(mappingLocations.get("/static/**").size(), equalTo(5));
		assertThat(getResourceResolvers("/static/**").size(), equalTo(1));
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
	public void resourceHandlerChainEnabled() throws Exception {
		load("spring.resources.chain.enabled:true");
		assertThat(getResourceResolvers("/webjars/**").size(), equalTo(2));
		assertThat(getResourceTransformers("/webjars/**").size(), equalTo(1));
		assertThat(getResourceResolvers("/**").size(), equalTo(2));
		assertThat(getResourceTransformers("/**").size(), equalTo(1));
		assertThat(getResourceResolvers("/**"), containsInstances(
				CachingResourceResolver.class, PathResourceResolver.class));
		assertThat(getResourceTransformers("/**"),
				contains(instanceOf(CachingResourceTransformer.class)));
	}

	@Test
	public void resourceHandlerFixedStrategyEnabled() throws Exception {
		load("spring.resources.chain.strategy.fixed.enabled:true",
				"spring.resources.chain.strategy.fixed.version:test",
				"spring.resources.chain.strategy.fixed.paths:/**/*.js");
		assertThat(getResourceResolvers("/webjars/**").size(), equalTo(3));
		assertThat(getResourceTransformers("/webjars/**").size(), equalTo(2));
		assertThat(getResourceResolvers("/**").size(), equalTo(3));
		assertThat(getResourceTransformers("/**").size(), equalTo(2));
		assertThat(getResourceResolvers("/**"),
				containsInstances(CachingResourceResolver.class,
						VersionResourceResolver.class, PathResourceResolver.class));
		assertThat(getResourceTransformers("/**"), containsInstances(
				CachingResourceTransformer.class, CssLinkResourceTransformer.class));
		VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(
				"/**").get(1);
		assertThat(resolver.getStrategyMap().get("/**/*.js"),
				instanceOf(FixedVersionStrategy.class));
	}

	@Test
	public void resourceHandlerContentStrategyEnabled() throws Exception {
		load("spring.resources.chain.strategy.content.enabled:true",
				"spring.resources.chain.strategy.content.paths:/**,/*.png");
		assertThat(getResourceResolvers("/webjars/**").size(), equalTo(3));
		assertThat(getResourceTransformers("/webjars/**").size(), equalTo(2));
		assertThat(getResourceResolvers("/**").size(), equalTo(3));
		assertThat(getResourceTransformers("/**").size(), equalTo(2));
		assertThat(getResourceResolvers("/**"),
				containsInstances(CachingResourceResolver.class,
						VersionResourceResolver.class, PathResourceResolver.class));
		assertThat(getResourceTransformers("/**"), containsInstances(
				CachingResourceTransformer.class, CssLinkResourceTransformer.class));
		VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(
				"/**").get(1);
		assertThat(resolver.getStrategyMap().get("/*.png"),
				instanceOf(ContentVersionStrategy.class));
	}

	@Test
	public void resourceHandlerChainCustomized() throws Exception {
		load("spring.resources.chain.enabled:true", "spring.resources.chain.cache:false",
				"spring.resources.chain.strategy.content.enabled:true",
				"spring.resources.chain.strategy.content.paths:/**,/*.png",
				"spring.resources.chain.strategy.fixed.enabled:true",
				"spring.resources.chain.strategy.fixed.version:test",
				"spring.resources.chain.strategy.fixed.paths:/**/*.js",
				"spring.resources.chain.html-application-cache:true");
		assertThat(getResourceResolvers("/webjars/**").size(), equalTo(2));
		assertThat(getResourceTransformers("/webjars/**").size(), equalTo(2));
		assertThat(getResourceResolvers("/**").size(), equalTo(2));
		assertThat(getResourceTransformers("/**").size(), equalTo(2));
		assertThat(getResourceResolvers("/**"), containsInstances(
				VersionResourceResolver.class, PathResourceResolver.class));
		assertThat(getResourceTransformers("/**"), containsInstances(
				CssLinkResourceTransformer.class, AppCacheManifestTransformer.class));
		VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(
				"/**").get(0);
		assertThat(resolver.getStrategyMap().get("/*.png"),
				instanceOf(ContentVersionStrategy.class));
		assertThat(resolver.getStrategyMap().get("/**/*.js"),
				instanceOf(FixedVersionStrategy.class));
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

	protected List<ResourceResolver> getResourceResolvers(String mapping) {
		SimpleUrlHandlerMapping handler = (SimpleUrlHandlerMapping) this.context
				.getBean("resourceHandlerMapping");
		ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler
				.getHandlerMap().get(mapping);
		return resourceHandler.getResourceResolvers();
	}

	protected List<ResourceTransformer> getResourceTransformers(String mapping) {
		SimpleUrlHandlerMapping handler = (SimpleUrlHandlerMapping) this.context
				.getBean("resourceHandlerMapping");
		ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler
				.getHandlerMap().get(mapping);
		return resourceHandler.getResourceTransformers();
	}

	@SuppressWarnings("unchecked")
	protected Map<String, List<Resource>> getMappingLocations(HandlerMapping mapping)
			throws IllegalAccessException {
		Map<String, List<Resource>> mappingLocations = new LinkedHashMap<String, List<Resource>>();
		if (mapping instanceof SimpleUrlHandlerMapping) {
			Field locationsField = ReflectionUtils
					.findField(ResourceHttpRequestHandler.class, "locations");
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
		assertThat(this.context.getBean("viewResolver"),
				instanceOf(MyViewResolver.class));
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
		assertThat(this.context.getBeansOfType(ResourceHttpRequestHandler.class)
				.get("faviconRequestHandler"), is(notNullValue()));
		assertThat(this.context.getBeansOfType(SimpleUrlHandlerMapping.class)
				.get("faviconHandlerMapping"), is(notNullValue()));
		Map<String, List<Resource>> mappingLocations = getFaviconMappingLocations();
		assertThat(mappingLocations.get("/**/favicon.ico").size(), equalTo(5));
	}

	@Test
	public void faviconMappingDisabled() throws IllegalAccessException {
		load("spring.mvc.favicon.enabled:false");
		assertThat(this.context.getBeansOfType(ResourceHttpRequestHandler.class)
				.get("faviconRequestHandler"), is(nullValue()));
		assertThat(this.context.getBeansOfType(SimpleUrlHandlerMapping.class)
				.get("faviconHandlerMapping"), is(nullValue()));
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

	@Test
	public void customMediaTypes() throws Exception {
		load("spring.mvc.mediaTypes.yaml:text/yaml");
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		ContentNegotiationManager actual = (ContentNegotiationManager) ReflectionTestUtils
				.getField(adapter, "contentNegotiationManager");
		assertTrue(actual.getAllFileExtensions().contains("yaml"));
	}

	@Test
	public void httpPutFormContentFilterIsAutoConfigured() {
		load();
		assertThat(
				this.context.getBeansOfType(OrderedHttpPutFormContentFilter.class).size(),
				is(equalTo(1)));
	}

	@Test
	public void httpPutFormContentFilterCanBeOverridden() {
		load(CustomHttpPutFormContentFilter.class);
		assertThat(
				this.context.getBeansOfType(OrderedHttpPutFormContentFilter.class).size(),
				is(equalTo(0)));
		assertThat(this.context.getBeansOfType(HttpPutFormContentFilter.class).size(),
				is(equalTo(1)));
	}

	@Test
	public void customConfigurableWebBindingInitializer() {
		load(CustomConfigurableWebBindingInitializer.class);
		assertThat(
				this.context.getBean(RequestMappingHandlerAdapter.class)
						.getWebBindingInitializer(),
				is(instanceOf(CustomWebBindingInitializer.class)));
	}

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <E> Matcher<E> containsInstances(Class<?>... types) {
		Matcher[] instances = new Matcher[types.length];
		for (int i = 0; i < instances.length; i++) {
			instances[i] = instanceOf(types[i]);
		}
		return contains(instances);
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
			registry.addResourceHandler("/webjars/**")
					.addResourceLocations("classpath:/foo/");
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

	@Configuration
	static class CustomConfigurableWebBindingInitializer {

		@Bean
		public ConfigurableWebBindingInitializer customConfigurableWebBindingInitializer() {
			return new CustomWebBindingInitializer();

		}

	}

	private static class CustomWebBindingInitializer
			extends ConfigurableWebBindingInitializer {
	}

	@Configuration
	static class CustomHttpPutFormContentFilter {

		@Bean
		public HttpPutFormContentFilter customHttpPutFormContentFilter() {
			return new HttpPutFormContentFilter();
		}

	}

}
