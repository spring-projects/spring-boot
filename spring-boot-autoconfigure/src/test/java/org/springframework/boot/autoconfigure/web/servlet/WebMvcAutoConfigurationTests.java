/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidatorFactory;

import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.WelcomePageHandlerMapping;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.boot.test.context.ServletWebContextLoader;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.filter.OrderedHttpPutFormContentFilter;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.AppCacheManifestTransformer;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.GzipResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.resource.VersionStrategy;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

	private static final MockServletWebServerFactory webServerFactory = new MockServletWebServerFactory();

	private final ServletWebContextLoader contextLoader = ContextLoader.servletWeb()
			.autoConfig(WebMvcAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class)
			.config(Config.class);

	@Test
	public void handlerAdaptersCreated() {
		this.contextLoader.load(context -> {
			assertThat(context.getBeanNamesForType(HandlerAdapter.class).length)
					.isEqualTo(3);
			assertThat(context.getBean(RequestMappingHandlerAdapter.class)
					.getMessageConverters()).isNotEmpty().isEqualTo(
							context.getBean(HttpMessageConverters.class).getConverters());
		});
	}

	@Test
	public void handlerMappingsCreated() {
		this.contextLoader.load(context -> {
			assertThat(context.getBeanNamesForType(HandlerMapping.class).length)
					.isEqualTo(7);
		});
	}

	@Test
	public void resourceHandlerMapping() {
		this.contextLoader.load(context -> {
			Map<String, List<Resource>> mappingLocations = getResourceMappingLocations(
					context);
			assertThat(mappingLocations.get("/**")).hasSize(5);
			assertThat(mappingLocations.get("/webjars/**")).hasSize(1);
			assertThat(mappingLocations.get("/webjars/**").get(0))
					.isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
			assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(1);
			assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(0);
			assertThat(getResourceResolvers(context, "/**")).hasSize(1);
			assertThat(getResourceTransformers(context, "/**")).hasSize(0);
		});
	}

	@Test
	public void customResourceHandlerMapping() {
		this.contextLoader.env("spring.mvc.static-path-pattern:/static/**")
				.load(context -> {
					Map<String, List<Resource>> mappingLocations = getResourceMappingLocations(
							context);
					assertThat(mappingLocations.get("/static/**")).hasSize(5);
					assertThat(getResourceResolvers(context, "/static/**")).hasSize(1);
				});
	}

	@Test
	public void resourceHandlerMappingOverrideWebjars() throws Exception {
		this.contextLoader.config(WebJars.class).load(context -> {
			Map<String, List<Resource>> mappingLocations = getResourceMappingLocations(
					context);
			assertThat(mappingLocations.get("/webjars/**")).hasSize(1);
			assertThat(mappingLocations.get("/webjars/**").get(0))
					.isEqualTo(new ClassPathResource("/foo/"));
		});
	}

	@Test
	public void resourceHandlerMappingOverrideAll() throws Exception {
		this.contextLoader.config(AllResources.class).load(context -> {
			Map<String, List<Resource>> mappingLocations = getResourceMappingLocations(
					context);
			assertThat(mappingLocations.get("/**")).hasSize(1);
			assertThat(mappingLocations.get("/**").get(0))
					.isEqualTo(new ClassPathResource("/foo/"));
		});
	}

	@Test
	public void resourceHandlerMappingDisabled() throws Exception {
		this.contextLoader.env("spring.resources.add-mappings:false").load(context -> {
			Map<String, List<Resource>> mappingLocations = getResourceMappingLocations(
					context);
			assertThat(mappingLocations.size()).isEqualTo(0);
		});
	}

	@Test
	public void resourceHandlerChainEnabled() throws Exception {
		this.contextLoader.env("spring.resources.chain.enabled:true").load(context -> {
			assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(2);
			assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(1);
			assertThat(getResourceResolvers(context, "/**"))
					.extractingResultOf("getClass").containsOnly(
							CachingResourceResolver.class, PathResourceResolver.class);
			assertThat(getResourceTransformers(context, "/**"))
					.extractingResultOf("getClass")
					.containsOnly(CachingResourceTransformer.class);
		});
	}

	@Test
	public void resourceHandlerFixedStrategyEnabled() throws Exception {
		this.contextLoader
				.env("spring.resources.chain.strategy.fixed.enabled:true",
						"spring.resources.chain.strategy.fixed.version:test",
						"spring.resources.chain.strategy.fixed.paths:/**/*.js")
				.load(context -> {
					assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
					assertThat(getResourceTransformers(context, "/webjars/**"))
							.hasSize(2);
					assertThat(getResourceResolvers(context, "/**"))
							.extractingResultOf("getClass")
							.containsOnly(CachingResourceResolver.class,
									VersionResourceResolver.class,
									PathResourceResolver.class);
					assertThat(getResourceTransformers(context, "/**"))
							.extractingResultOf("getClass")
							.containsOnly(CachingResourceTransformer.class,
									CssLinkResourceTransformer.class);
					VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(
							context, "/**").get(1);
					assertThat(resolver.getStrategyMap().get("/**/*.js"))
							.isInstanceOf(FixedVersionStrategy.class);
				});
	}

	@Test
	public void resourceHandlerContentStrategyEnabled() throws Exception {
		this.contextLoader
				.env("spring.resources.chain.strategy.content.enabled:true",
						"spring.resources.chain.strategy.content.paths:/**,/*.png")
				.load(context -> {
					assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
					assertThat(getResourceTransformers(context, "/webjars/**"))
							.hasSize(2);
					assertThat(getResourceResolvers(context, "/**"))
							.extractingResultOf("getClass")
							.containsOnly(CachingResourceResolver.class,
									VersionResourceResolver.class,
									PathResourceResolver.class);
					assertThat(getResourceTransformers(context, "/**"))
							.extractingResultOf("getClass")
							.containsOnly(CachingResourceTransformer.class,
									CssLinkResourceTransformer.class);
					VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(
							context, "/**").get(1);
					assertThat(resolver.getStrategyMap().get("/*.png"))
							.isInstanceOf(ContentVersionStrategy.class);
				});
	}

	@Test
	public void resourceHandlerChainCustomized() {
		this.contextLoader.env("spring.resources.chain.enabled:true",
				"spring.resources.chain.cache:false",
				"spring.resources.chain.strategy.content.enabled:true",
				"spring.resources.chain.strategy.content.paths:/**,/*.png",
				"spring.resources.chain.strategy.fixed.enabled:true",
				"spring.resources.chain.strategy.fixed.version:test",
				"spring.resources.chain.strategy.fixed.paths:/**/*.js",
				"spring.resources.chain.html-application-cache:true",
				"spring.resources.chain.gzipped:true");
		this.contextLoader.load(context -> {
			assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
			assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(2);
			assertThat(getResourceResolvers(context, "/**"))
					.extractingResultOf("getClass")
					.containsOnly(VersionResourceResolver.class,
							GzipResourceResolver.class, PathResourceResolver.class);
			assertThat(getResourceTransformers(context, "/**"))
					.extractingResultOf("getClass")
					.containsOnly(CssLinkResourceTransformer.class,
							AppCacheManifestTransformer.class);
			VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(
					context, "/**").get(0);
			Map<String, VersionStrategy> strategyMap = resolver.getStrategyMap();
			assertThat(strategyMap.get("/*.png"))
					.isInstanceOf(ContentVersionStrategy.class);
			assertThat(strategyMap.get("/**/*.js"))
					.isInstanceOf(FixedVersionStrategy.class);
		});
	}

	@Test
	public void noLocaleResolver() throws Exception {
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(LocaleResolver.class)).isEmpty();
		});
	}

	@Test
	public void overrideLocale() throws Exception {
		this.contextLoader.env("spring.mvc.locale:en_UK",
				"spring.mvc.locale-resolver=fixed");
		this.contextLoader.load(context -> {
			// mock request and set user preferred locale
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
			request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "nl_NL");
			LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
			assertThat(localeResolver).isInstanceOf(FixedLocaleResolver.class);
			Locale locale = localeResolver.resolveLocale(request);
			// test locale resolver uses fixed locale and not user preferred locale
			assertThat(locale.toString()).isEqualTo("en_UK");
		});
	}

	@Test
	public void useAcceptHeaderLocale() {
		this.contextLoader.env("spring.mvc.locale:en_UK").load(context -> {
			// mock request and set user preferred locale
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
			request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "nl_NL");
			LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
			assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
			Locale locale = localeResolver.resolveLocale(request);
			// test locale resolver uses user preferred locale
			assertThat(locale.toString()).isEqualTo("nl_NL");
		});
	}

	@Test
	public void useDefaultLocaleIfAcceptHeaderNoSet() {
		this.contextLoader.env("spring.mvc.locale:en_UK").load(context -> {
			// mock request and set user preferred locale
			MockHttpServletRequest request = new MockHttpServletRequest();
			LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
			assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
			Locale locale = localeResolver.resolveLocale(request);
			// test locale resolver uses default locale if no header is set
			assertThat(locale.toString()).isEqualTo("en_UK");
		});
	}

	@Test
	public void noDateFormat() {
		this.contextLoader.load(context -> {
			FormattingConversionService conversionService = context
					.getBean(FormattingConversionService.class);
			Date date = new DateTime(1988, 6, 25, 20, 30).toDate();
			// formatting conversion service should use simple toString()
			assertThat(conversionService.convert(date, String.class))
					.isEqualTo(date.toString());
		});
	}

	@Test
	public void overrideDateFormat() {
		this.contextLoader.env("spring.mvc.date-format:dd*MM*yyyy").load(context -> {
			FormattingConversionService conversionService = context
					.getBean(FormattingConversionService.class);
			Date date = new DateTime(1988, 6, 25, 20, 30).toDate();
			assertThat(conversionService.convert(date, String.class))
					.isEqualTo("25*06*1988");
		});
	}

	@Test
	public void noMessageCodesResolver() {
		this.contextLoader.load(context -> {
			assertThat(context.getBean(WebMvcAutoConfigurationAdapter.class)
					.getMessageCodesResolver()).isNull();
		});
	}

	@Test
	public void overrideMessageCodesFormat() {
		this.contextLoader
				.env("spring.mvc.messageCodesResolverFormat:POSTFIX_ERROR_CODE");
		this.contextLoader.load(context -> {
			assertThat(context.getBean(WebMvcAutoConfigurationAdapter.class)
					.getMessageCodesResolver()).isNotNull();
		});
	}

	@Test
	public void ignoreDefaultModelOnRedirectIsTrue() {
		this.contextLoader.load(context -> {
			RequestMappingHandlerAdapter adapter = context
					.getBean(RequestMappingHandlerAdapter.class);
			assertThat(adapter).extracting("ignoreDefaultModelOnRedirect")
					.containsExactly(true);
		});
	}

	@Test
	public void overrideIgnoreDefaultModelOnRedirect() {
		this.contextLoader.env("spring.mvc.ignore-default-model-on-redirect:false");
		this.contextLoader.load(context -> {
			RequestMappingHandlerAdapter adapter = context
					.getBean(RequestMappingHandlerAdapter.class);
			assertThat(adapter).extracting("ignoreDefaultModelOnRedirect")
					.containsExactly(false);
		});
	}

	@Test
	public void customViewResolver() {
		this.contextLoader.config(CustomViewResolver.class).load(context -> {
			assertThat(context.getBean("viewResolver"))
					.isInstanceOf(MyViewResolver.class);
		});
	}

	@Test
	public void customContentNegotiatingViewResolver() throws Exception {
		this.contextLoader.config(CustomContentNegotiatingViewResolver.class);
		this.contextLoader.load(context -> {
			Map<String, ContentNegotiatingViewResolver> beans = context
					.getBeansOfType(ContentNegotiatingViewResolver.class);
			assertThat(beans.size()).isEqualTo(1);
			assertThat(beans.keySet().iterator().next()).isEqualTo("myViewResolver");
		});
	}

	@Test
	public void faviconMapping() {
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(ResourceHttpRequestHandler.class)
					.get("faviconRequestHandler")).isNotNull();
			assertThat(context.getBeansOfType(SimpleUrlHandlerMapping.class)
					.get("faviconHandlerMapping")).isNotNull();
			Map<String, List<Resource>> mappingLocations = getFaviconMappingLocations(
					context);
			assertThat(mappingLocations.get("/**/favicon.ico")).hasSize(6);
		});
	}

	@Test
	public void faviconMappingUsesStaticLocations() {
		this.contextLoader.env("spring.resources.static-locations=classpath:/static");
		this.contextLoader.load(context -> {
			assertThat(getFaviconMappingLocations(context).get("/**/favicon.ico"))
					.hasSize(2);
		});
	}

	@Test
	public void faviconMappingDisabled() throws IllegalAccessException {
		this.contextLoader.env("spring.mvc.favicon.enabled:false").load(context -> {
			assertThat(context.getBeansOfType(ResourceHttpRequestHandler.class)
					.get("faviconRequestHandler")).isNull();
			assertThat(context.getBeansOfType(SimpleUrlHandlerMapping.class)
					.get("faviconHandlerMapping")).isNull();
		});
	}

	@Test
	public void defaultAsyncRequestTimeout() throws Exception {
		this.contextLoader.load(context -> {
			RequestMappingHandlerAdapter adapter = context
					.getBean(RequestMappingHandlerAdapter.class);
			assertThat(ReflectionTestUtils.getField(adapter, "asyncRequestTimeout"))
					.isNull();
		});
	}

	@Test
	public void customAsyncRequestTimeout() throws Exception {
		this.contextLoader.env("spring.mvc.async.request-timeout:12345").load(context -> {
			RequestMappingHandlerAdapter adapter = context
					.getBean(RequestMappingHandlerAdapter.class);
			assertThat(ReflectionTestUtils.getField(adapter, "asyncRequestTimeout"))
					.isEqualTo(12345L);
		});
	}

	@Test
	public void customMediaTypes() throws Exception {
		this.contextLoader.env("spring.mvc.mediaTypes.yaml:text/yaml").load(context -> {
			RequestMappingHandlerAdapter adapter = context
					.getBean(RequestMappingHandlerAdapter.class);
			ContentNegotiationManager contentNegotiationManager = (ContentNegotiationManager) ReflectionTestUtils
					.getField(adapter, "contentNegotiationManager");
			assertThat(contentNegotiationManager.getAllFileExtensions()).contains("yaml");
		});
	}

	@Test
	public void httpPutFormContentFilterIsAutoConfigured() {
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(OrderedHttpPutFormContentFilter.class))
					.hasSize(1);
		});
	}

	@Test
	public void httpPutFormContentFilterCanBeOverridden() {
		this.contextLoader.config(CustomHttpPutFormContentFilter.class).load(context -> {
			assertThat(context.getBeansOfType(OrderedHttpPutFormContentFilter.class))
					.hasSize(0);
			assertThat(context.getBeansOfType(HttpPutFormContentFilter.class)).hasSize(1);
		});
	}

	@Test
	public void httpPutFormContentFilterCanBeDisabled() throws Exception {
		this.contextLoader.env("spring.mvc.formcontent.putfilter.enabled=false");
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(HttpPutFormContentFilter.class)).isEmpty();
		});
	}

	@Test
	public void customConfigurableWebBindingInitializer() {
		this.contextLoader.config(CustomConfigurableWebBindingInitializer.class);
		this.contextLoader.load(context -> {
			assertThat(context.getBean(RequestMappingHandlerAdapter.class)
					.getWebBindingInitializer())
							.isInstanceOf(CustomWebBindingInitializer.class);
		});
	}

	@Test
	public void customRequestMappingHandlerMapping() {
		this.contextLoader.config(CustomRequestMappingHandlerMapping.class);
		this.contextLoader.load(context -> {
			assertThat(context.getBean(RequestMappingHandlerMapping.class))
					.isInstanceOf(MyRequestMappingHandlerMapping.class);
		});
	}

	@Test
	public void customRequestMappingHandlerAdapter() {
		this.contextLoader.config(CustomRequestMappingHandlerAdapter.class);
		this.contextLoader.load(context -> {
			assertThat(context.getBean(RequestMappingHandlerAdapter.class))
					.isInstanceOf(MyRequestMappingHandlerAdapter.class);
		});
	}

	@Test
	public void multipleWebMvcRegistrations() {
		this.contextLoader.config(MultipleWebMvcRegistrations.class).load(context -> {
			assertThat(context.getBean(RequestMappingHandlerMapping.class))
					.isNotInstanceOf(MyRequestMappingHandlerMapping.class);
			assertThat(context.getBean(RequestMappingHandlerAdapter.class))
					.isNotInstanceOf(MyRequestMappingHandlerAdapter.class);
		});
	}

	@Test
	public void defaultLogResolvedException() {
		this.contextLoader.load(context -> {
			assertExceptionResolverWarnLoggers(context,
					warnLogger -> assertThat(warnLogger).isNull());
		});
	}

	@Test
	public void customLogResolvedException() {
		this.contextLoader.env("spring.mvc.log-resolved-exception:true").load(context -> {
			assertExceptionResolverWarnLoggers(context,
					warnLogger -> assertThat(warnLogger).isNotNull());
		});
	}

	@Test
	public void welcomePageMappingProducesNotFoundResponseWhenThereIsNoWelcomePage() {
		this.contextLoader
				.env("spring.resources.static-locations:classpath:/no-welcome-page/");
		this.contextLoader.loadWeb(context -> {
			assertThat(context.getBeansOfType(WelcomePageHandlerMapping.class))
					.hasSize(1);
			MockMvcBuilders.webAppContextSetup(context).build()
					.perform(get("/").accept(MediaType.TEXT_HTML))
					.andExpect(status().isNotFound());
		});
	}

	@Test
	public void welcomePageRootHandlerIsNotRegisteredWhenStaticPathPatternIsNotSlashStarStar() {
		this.contextLoader.env(
				"spring.resources.static-locations:classpath:/welcome-page/",
				"spring.mvc.static-path-pattern:/foo/**");
		this.contextLoader.load(context -> {
			WelcomePageHandlerMapping welcomePageHandlerMapping = context
					.getBean(WelcomePageHandlerMapping.class);
			assertThat(welcomePageHandlerMapping.getRootHandler()).isNull();
		});
	}

	@Test
	public void welcomePageMappingHandlesRequestsThatAcceptTextHtml() {
		this.contextLoader
				.env("spring.resources.static-locations:classpath:/welcome-page/");
		this.contextLoader.loadWeb(context -> {
			assertThat(context.getBeansOfType(WelcomePageHandlerMapping.class))
					.hasSize(1);
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
					.andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
			mockMvc.perform(get("/").accept("*/*")).andExpect(status().isOk())
					.andExpect(forwardedUrl("index.html"));
		});
	}

	@Test
	public void welcomePageMappingDoesNotHandleRequestsThatDoNotAcceptTextHtml() {
		this.contextLoader
				.env("spring.resources.static-locations:classpath:/welcome-page/");
		this.contextLoader.loadWeb(context -> {
			assertThat(context.getBeansOfType(WelcomePageHandlerMapping.class))
					.hasSize(1);
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound());
		});
	}

	@Test
	public void welcomePageMappingHandlesRequestsWithNoAcceptHeader() {
		this.contextLoader
				.env("spring.resources.static-locations:classpath:/welcome-page/");
		this.contextLoader.loadWeb(context -> {
			assertThat(context.getBeansOfType(WelcomePageHandlerMapping.class))
					.hasSize(1);
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/")).andExpect(status().isOk())
					.andExpect(forwardedUrl("index.html"));
		});
	}

	@Test
	public void welcomePageMappingHandlesRequestsWithEmptyAcceptHeader()
			throws Exception {
		this.contextLoader
				.env("spring.resources.static-locations:classpath:/welcome-page/");
		this.contextLoader.loadWeb(context -> {
			assertThat(context.getBeansOfType(WelcomePageHandlerMapping.class))
					.hasSize(1);
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").header(HttpHeaders.ACCEPT, ""))
					.andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
		});
	}

	@Test
	public void welcomePageMappingWorksWithNoTrailingSlashOnResourceLocation()
			throws Exception {
		this.contextLoader
				.env("spring.resources.static-locations:classpath:/welcome-page");
		this.contextLoader.loadWeb(context -> {
			assertThat(context.getBeansOfType(WelcomePageHandlerMapping.class))
					.hasSize(1);
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
			mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
					.andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
		});

	}

	@Test
	public void validatorWhenNoValidatorShouldUseDefault() {
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(ValidatorFactory.class)).isEmpty();
			assertThat(context.getBeansOfType(javax.validation.Validator.class))
					.isEmpty();
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(springValidatorBeans).containsExactly("mvcValidator");
		});
	}

	@Test
	public void validatorWhenNoCustomizationShouldUseAutoConfigured() {
		this.contextLoader.autoConfigFirst(ValidationAutoConfiguration.class);
		this.contextLoader.load(context -> {
			String[] jsrValidatorBeans = context
					.getBeanNamesForType(javax.validation.Validator.class);
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(jsrValidatorBeans).containsExactly("defaultValidator");
			assertThat(springValidatorBeans).containsExactly("defaultValidator",
					"mvcValidator");
			Validator validator = context.getBean("mvcValidator", Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			Object defaultValidator = context.getBean("defaultValidator");
			assertThat(((ValidatorAdapter) validator).getTarget())
					.isSameAs(defaultValidator);
			// Primary Spring validator is the one used by MVC behind the scenes
			assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
		});
	}

	@Test
	public void validatorWithConfigurerShouldUseSpringValidator() {
		this.contextLoader.config(MvcValidator.class).load(context -> {
			assertThat(context.getBeansOfType(ValidatorFactory.class)).isEmpty();
			assertThat(context.getBeansOfType(javax.validation.Validator.class))
					.isEmpty();
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(springValidatorBeans).containsExactly("mvcValidator");
			assertThat(context.getBean("mvcValidator"))
					.isSameAs(context.getBean(MvcValidator.class).validator);
		});
	}

	@Test
	public void validatorWithConfigurerDoesNotExposeJsr303() {
		this.contextLoader.config(MvcJsr303Validator.class).load(context -> {
			assertThat(context.getBeansOfType(ValidatorFactory.class)).isEmpty();
			assertThat(context.getBeansOfType(javax.validation.Validator.class))
					.isEmpty();
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(springValidatorBeans).containsExactly("mvcValidator");
			Validator validator = context.getBean("mvcValidator", Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			assertThat(((ValidatorAdapter) validator).getTarget())
					.isSameAs(context.getBean(MvcJsr303Validator.class).validator);
		});
	}

	@Test
	public void validatorWithConfigurerTakesPrecedence() {
		this.contextLoader.autoConfigFirst(ValidationAutoConfiguration.class)
				.config(MvcValidator.class);
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(ValidatorFactory.class)).hasSize(1);
			assertThat(context.getBeansOfType(javax.validation.Validator.class))
					.hasSize(1);
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(springValidatorBeans).containsExactly("defaultValidator",
					"mvcValidator");
			assertThat(context.getBean("mvcValidator"))
					.isSameAs(context.getBean(MvcValidator.class).validator);
			// Primary Spring validator is the auto-configured one as the MVC one has been
			// customized via a WebMvcConfigurer
			assertThat(context.getBean(Validator.class))
					.isEqualTo(context.getBean("defaultValidator"));
		});

	}

	@Test
	public void validatorWithCustomSpringValidatorIgnored() {
		this.contextLoader.autoConfigFirst(ValidationAutoConfiguration.class)
				.config(CustomSpringValidator.class);
		this.contextLoader.load(context -> {
			String[] jsrValidatorBeans = context
					.getBeanNamesForType(javax.validation.Validator.class);
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(jsrValidatorBeans).containsExactly("defaultValidator");
			assertThat(springValidatorBeans).containsExactly("customSpringValidator",
					"defaultValidator", "mvcValidator");
			Validator validator = context.getBean("mvcValidator", Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			Object defaultValidator = context.getBean("defaultValidator");
			assertThat(((ValidatorAdapter) validator).getTarget())
					.isSameAs(defaultValidator);
			// Primary Spring validator is the one used by MVC behind the scenes
			assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
		});

	}

	@Test
	public void validatorWithCustomJsr303ValidatorExposedAsSpringValidator() {
		this.contextLoader.autoConfigFirst(ValidationAutoConfiguration.class)
				.config(CustomJsr303Validator.class);
		this.contextLoader.load(context -> {
			assertThat(context.getBeansOfType(ValidatorFactory.class)).isEmpty();
			String[] jsrValidatorBeans = context
					.getBeanNamesForType(javax.validation.Validator.class);
			String[] springValidatorBeans = context.getBeanNamesForType(Validator.class);
			assertThat(jsrValidatorBeans).containsExactly("customJsr303Validator");
			assertThat(springValidatorBeans).containsExactly("mvcValidator");
			Validator validator = context.getBean(Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			Validator target = ((ValidatorAdapter) validator).getTarget();
			assertThat(ReflectionTestUtils.getField(target, "targetValidator"))
					.isSameAs(context.getBean("customJsr303Validator"));
		});
	}

	@Test
	public void httpMessageConverterThatUsesConversionServiceDoesNotCreateACycle() {
		// TODO load(ContextConsumer...) or load()
		this.contextLoader.config(CustomHttpMessageConverter.class).load(context -> {
		});
	}

	private void assertExceptionResolverWarnLoggers(ApplicationContext context,
			Consumer<Object> warnLogger) {
		HandlerExceptionResolver exceptionResolver = context
				.getBean(HandlerExceptionResolver.class);
		assertThat(exceptionResolver)
				.isInstanceOf(HandlerExceptionResolverComposite.class);
		List<HandlerExceptionResolver> delegates = ((HandlerExceptionResolverComposite) exceptionResolver)
				.getExceptionResolvers();
		for (HandlerExceptionResolver delegate : delegates) {
			if (delegate instanceof AbstractHandlerExceptionResolver) {
				warnLogger.accept(ReflectionTestUtils.getField(delegate, "warnLogger"));
			}
		}
	}

	protected Map<String, List<Resource>> getFaviconMappingLocations(
			ApplicationContext context) {
		HandlerMapping mapping = (HandlerMapping) context
				.getBean("faviconHandlerMapping");
		return getMappingLocations(mapping);
	}

	protected Map<String, List<Resource>> getResourceMappingLocations(
			ApplicationContext context) throws IllegalAccessException {
		HandlerMapping mapping = (HandlerMapping) context
				.getBean("resourceHandlerMapping");
		return getMappingLocations(mapping);
	}

	protected List<ResourceResolver> getResourceResolvers(ApplicationContext context,
			String mapping) {
		SimpleUrlHandlerMapping handler = (SimpleUrlHandlerMapping) context
				.getBean("resourceHandlerMapping");
		ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler
				.getHandlerMap().get(mapping);
		return resourceHandler.getResourceResolvers();
	}

	protected List<ResourceTransformer> getResourceTransformers(
			ApplicationContext context, String mapping) {
		SimpleUrlHandlerMapping handler = (SimpleUrlHandlerMapping) context
				.getBean("resourceHandlerMapping");
		ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler
				.getHandlerMap().get(mapping);
		return resourceHandler.getResourceTransformers();
	}

	@SuppressWarnings("unchecked")
	protected Map<String, List<Resource>> getMappingLocations(HandlerMapping mapping) {
		Map<String, List<Resource>> mappingLocations = new LinkedHashMap<>();
		if (mapping instanceof SimpleUrlHandlerMapping) {
			((SimpleUrlHandlerMapping) mapping).getHandlerMap().forEach((key, value) -> {
				mappingLocations.put(key, (List<Resource>) ReflectionTestUtils
						.getField(value, "locations"));
			});
		}
		return mappingLocations;
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
	protected static class WebJars implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/webjars/**")
					.addResourceLocations("classpath:/foo/");
		}

	}

	@Configuration
	protected static class AllResources implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/**").addResourceLocations("classpath:/foo/");
		}

	}

	@Configuration
	public static class Config {

		@Bean
		public ServletWebServerFactory webServerFactory() {
			return webServerFactory;
		}

		@Bean
		public WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
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

	@Configuration
	static class CustomRequestMappingHandlerMapping {

		@Bean
		public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
			return new WebMvcRegistrations() {

				@Override
				public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
					return new MyRequestMappingHandlerMapping();
				}

			};
		}

	}

	private static class MyRequestMappingHandlerMapping
			extends RequestMappingHandlerMapping {

	}

	@Configuration
	static class CustomRequestMappingHandlerAdapter {

		@Bean
		public WebMvcRegistrations webMvcRegistrationsHandlerAdapter() {
			return new WebMvcRegistrations() {

				@Override
				public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
					return new MyRequestMappingHandlerAdapter();
				}

			};
		}

	}

	private static class MyRequestMappingHandlerAdapter
			extends RequestMappingHandlerAdapter {

	}

	@Configuration
	@Import({ CustomRequestMappingHandlerMapping.class,
			CustomRequestMappingHandlerAdapter.class })
	static class MultipleWebMvcRegistrations {

	}

	@Configuration
	protected static class MvcValidator implements WebMvcConfigurer {

		private final Validator validator = mock(Validator.class);

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration
	protected static class MvcJsr303Validator implements WebMvcConfigurer {

		private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration
	static class CustomJsr303Validator {

		@Bean
		public javax.validation.Validator customJsr303Validator() {
			return mock(javax.validation.Validator.class);
		}

	}

	@Configuration
	static class CustomSpringValidator {

		@Bean
		public Validator customSpringValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class CustomHttpMessageConverter {

		@Bean
		public HttpMessageConverter<?> customHttpMessageConverter(
				ConversionService conversionService) {
			return mock(HttpMessageConverter.class);
		}

	}

}
