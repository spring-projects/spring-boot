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

package org.springframework.boot.webmvc.autoconfigure;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidatorFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.servlet.filter.OrderedFormContentFilter;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidatorAdapter;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.server.servlet.MockServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfigurationTests.OrderedControllerAdviceBeansConfiguration.HighestOrderedControllerAdvice;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfigurationTests.OrderedControllerAdviceBeansConfiguration.LowestOrderedControllerAdvice;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.RequestPath;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.resource.VersionStrategy;
import org.springframework.web.servlet.support.AbstractFlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebMvcAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Eddú Meléndez
 * @author Kristine Jetzke
 * @author Artsiom Yudovin
 * @author Scott Frederick
 * @author Vedran Pavic
 */
class WebMvcAutoConfigurationTests {

	private static final MockServletWebServerFactory webServerFactory = new MockServletWebServerFactory();

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
						HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
		.withUserConfiguration(Config.class);

	@Test
	void handlerAdaptersCreated() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(HandlerAdapter.class).hasSize(4);
			assertThat(context.getBean(RequestMappingHandlerAdapter.class).getMessageConverters()).isNotEmpty()
				.isEqualTo(context.getBean(HttpMessageConverters.class).getConverters());
		});
	}

	@Test
	void handlerMappingsCreated() {
		this.contextRunner.run((context) -> assertThat(context).getBeans(HandlerMapping.class).hasSize(6));
	}

	@Test
	void resourceHandlerMapping() {
		this.contextRunner.run((context) -> {
			Map<String, List<Resource>> locations = getResourceMappingLocations(context);
			assertThat(locations.get("/**")).hasSize(5);
			assertThat(locations.get("/webjars/**")).hasSize(1);
			assertThat(locations.get("/webjars/**").get(0))
				.isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
			assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(1);
			assertThat(getResourceTransformers(context, "/webjars/**")).isEmpty();
			assertThat(getResourceResolvers(context, "/**")).hasSize(1);
			assertThat(getResourceTransformers(context, "/**")).isEmpty();
		});
	}

	@Test
	void customResourceHandlerMapping() {
		this.contextRunner.withPropertyValues("spring.mvc.static-path-pattern:/static/**").run((context) -> {
			Map<String, List<Resource>> locations = getResourceMappingLocations(context);
			assertThat(locations.get("/static/**")).hasSize(5);
			assertThat(getResourceResolvers(context, "/static/**")).hasSize(1);
		});
	}

	@Test
	void customWebjarsHandlerMapping() {
		this.contextRunner.withPropertyValues("spring.mvc.webjars-path-pattern:/assets/**").run((context) -> {
			Map<String, List<Resource>> locations = getResourceMappingLocations(context);
			assertThat(locations.get("/assets/**")).hasSize(1);
			assertThat(locations.get("/assets/**").get(0))
				.isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
			assertThat(getResourceResolvers(context, "/assets/**")).hasSize(1);
		});
	}

	@Test
	void resourceHandlerMappingOverrideWebjars() {
		this.contextRunner.withUserConfiguration(WebJars.class).run((context) -> {
			Map<String, List<Resource>> locations = getResourceMappingLocations(context);
			assertThat(locations.get("/webjars/**")).hasSize(1);
			assertThat(locations.get("/webjars/**").get(0)).isEqualTo(new ClassPathResource("/foo/"));
		});
	}

	@Test
	void resourceHandlerMappingOverrideAll() {
		this.contextRunner.withUserConfiguration(AllResources.class).run((context) -> {
			Map<String, List<Resource>> locations = getResourceMappingLocations(context);
			assertThat(locations.get("/**")).hasSize(1);
			assertThat(locations.get("/**").get(0)).isEqualTo(new ClassPathResource("/foo/"));
		});
	}

	@Test
	void resourceHandlerMappingDisabled() {
		this.contextRunner.withPropertyValues("spring.web.resources.add-mappings:false")
			.run((context) -> assertThat(getResourceMappingLocations(context)).isEmpty());
	}

	@Test
	void resourceHandlerChainEnabled() {
		this.contextRunner.withPropertyValues("spring.web.resources.chain.enabled:true").run((context) -> {
			assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(2);
			assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(1);
			assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass")
				.containsOnly(CachingResourceResolver.class, PathResourceResolver.class);
			assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
				.containsOnly(CachingResourceTransformer.class);
		});
	}

	@Test
	void resourceHandlerFixedStrategyEnabled() {
		this.contextRunner
			.withPropertyValues("spring.web.resources.chain.strategy.fixed.enabled:true",
					"spring.web.resources.chain.strategy.fixed.version:test",
					"spring.web.resources.chain.strategy.fixed.paths:/**/*.js")
			.run((context) -> {
				assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
				assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(2);
				assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass")
					.containsOnly(CachingResourceResolver.class, VersionResourceResolver.class,
							PathResourceResolver.class);
				assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
					.containsOnly(CachingResourceTransformer.class, CssLinkResourceTransformer.class);
				VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(context, "/**")
					.get(1);
				assertThat(resolver.getStrategyMap().get("/**/*.js")).isInstanceOf(FixedVersionStrategy.class);
			});
	}

	@Test
	void resourceHandlerContentStrategyEnabled() {
		this.contextRunner
			.withPropertyValues("spring.web.resources.chain.strategy.content.enabled:true",
					"spring.web.resources.chain.strategy.content.paths:/**,/*.png")
			.run((context) -> {
				assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
				assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(2);
				assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass")
					.containsOnly(CachingResourceResolver.class, VersionResourceResolver.class,
							PathResourceResolver.class);
				assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
					.containsOnly(CachingResourceTransformer.class, CssLinkResourceTransformer.class);
				VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(context, "/**")
					.get(1);
				assertThat(resolver.getStrategyMap().get("/*.png")).isInstanceOf(ContentVersionStrategy.class);
			});
	}

	@Test
	void resourceHandlerChainCustomized() {
		this.contextRunner.withPropertyValues("spring.web.resources.chain.enabled:true",
				"spring.web.resources.chain.cache:false", "spring.web.resources.chain.strategy.content.enabled:true",
				"spring.web.resources.chain.strategy.content.paths:/**,/*.png",
				"spring.web.resources.chain.strategy.fixed.enabled:true",
				"spring.web.resources.chain.strategy.fixed.version:test",
				"spring.web.resources.chain.strategy.fixed.paths:/**/*.js",
				"spring.web.resources.chain.html-application-cache:true", "spring.web.resources.chain.compressed:true")
			.run((context) -> {
				assertThat(getResourceResolvers(context, "/webjars/**")).hasSize(3);
				assertThat(getResourceTransformers(context, "/webjars/**")).hasSize(1);
				assertThat(getResourceResolvers(context, "/**")).extractingResultOf("getClass")
					.containsOnly(EncodedResourceResolver.class, VersionResourceResolver.class,
							PathResourceResolver.class);
				assertThat(getResourceTransformers(context, "/**")).extractingResultOf("getClass")
					.containsOnly(CssLinkResourceTransformer.class);
				VersionResourceResolver resolver = (VersionResourceResolver) getResourceResolvers(context, "/**")
					.get(1);
				Map<String, VersionStrategy> strategyMap = resolver.getStrategyMap();
				assertThat(strategyMap.get("/*.png")).isInstanceOf(ContentVersionStrategy.class);
				assertThat(strategyMap.get("/**/*.js")).isInstanceOf(FixedVersionStrategy.class);
			});
	}

	@Test
	void defaultLocaleResolver() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(LocaleResolver.class);
			LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
			assertThat(localeResolver).hasFieldOrPropertyWithValue("defaultLocale", null);
		});
	}

	@Test
	void overrideLocale() {
		this.contextRunner.withPropertyValues("spring.web.locale:en_UK", "spring.web.locale-resolver=fixed")
			.run((loader) -> {
				// mock request and set user preferred locale
				MockHttpServletRequest request = new MockHttpServletRequest();
				request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
				request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "nl_NL");
				LocaleResolver localeResolver = loader.getBean(LocaleResolver.class);
				assertThat(localeResolver).isInstanceOf(FixedLocaleResolver.class);
				Locale locale = localeResolver.resolveLocale(request);
				// test locale resolver uses fixed locale and not user preferred
				// locale
				assertThat(locale).hasToString("en_UK");
			});
	}

	@Test
	void useAcceptHeaderLocale() {
		this.contextRunner.withPropertyValues("spring.web.locale:en_UK").run((loader) -> {
			// mock request and set user preferred locale
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
			request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "nl_NL");
			LocaleResolver localeResolver = loader.getBean(LocaleResolver.class);
			assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
			Locale locale = localeResolver.resolveLocale(request);
			// test locale resolver uses user preferred locale
			assertThat(locale).hasToString("nl_NL");
		});
	}

	@Test
	void useDefaultLocaleIfAcceptHeaderNoSet() {
		this.contextRunner.withPropertyValues("spring.web.locale:en_UK").run((context) -> {
			// mock request and set user preferred locale
			MockHttpServletRequest request = new MockHttpServletRequest();
			LocaleResolver localeResolver = context.getBean(LocaleResolver.class);
			assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
			Locale locale = localeResolver.resolveLocale(request);
			// test locale resolver uses default locale if no header is set
			assertThat(locale).hasToString("en_UK");
		});
	}

	@Test
	void customLocaleResolverWithMatchingNameReplacesAutoConfiguredLocaleResolver() {
		this.contextRunner.withBean("localeResolver", CustomLocaleResolver.class, CustomLocaleResolver::new)
			.run((context) -> {
				assertThat(context).hasSingleBean(LocaleResolver.class);
				assertThat(context.getBean("localeResolver")).isInstanceOf(CustomLocaleResolver.class);
			});
	}

	@Test
	void customLocaleResolverWithDifferentNameDoesNotReplaceAutoConfiguredLocaleResolver() {
		this.contextRunner.withBean("customLocaleResolver", CustomLocaleResolver.class, CustomLocaleResolver::new)
			.run((context) -> {
				assertThat(context.getBean("customLocaleResolver")).isInstanceOf(CustomLocaleResolver.class);
				assertThat(context.getBean("localeResolver")).isInstanceOf(AcceptHeaderLocaleResolver.class);
			});
	}

	@Test
	void customFlashMapManagerWithMatchingNameReplacesDefaultFlashMapManager() {
		this.contextRunner.withBean("flashMapManager", CustomFlashMapManager.class, CustomFlashMapManager::new)
			.run((context) -> {
				assertThat(context).hasSingleBean(FlashMapManager.class);
				assertThat(context.getBean("flashMapManager")).isInstanceOf(CustomFlashMapManager.class);
			});
	}

	@Test
	void customFlashMapManagerWithDifferentNameDoesNotReplaceDefaultFlashMapManager() {
		this.contextRunner.withBean("customFlashMapManager", CustomFlashMapManager.class, CustomFlashMapManager::new)
			.run((context) -> {
				assertThat(context.getBean("customFlashMapManager")).isInstanceOf(CustomFlashMapManager.class);
				assertThat(context.getBean("flashMapManager")).isInstanceOf(SessionFlashMapManager.class);
			});
	}

	@Test
	void customViewNameTranslatorWithMatchingNameReplacesDefaultViewNameTranslator() {
		this.contextRunner.withBean("viewNameTranslator", CustomViewNameTranslator.class, CustomViewNameTranslator::new)
			.run((context) -> {
				assertThat(context).hasSingleBean(RequestToViewNameTranslator.class);
				assertThat(context.getBean("viewNameTranslator")).isInstanceOf(CustomViewNameTranslator.class);
			});
	}

	@Test
	void customViewNameTranslatorWithDifferentNameDoesNotReplaceDefaultViewNameTranslator() {
		this.contextRunner
			.withBean("customViewNameTranslator", CustomViewNameTranslator.class, CustomViewNameTranslator::new)
			.run((context) -> {
				assertThat(context.getBean("customViewNameTranslator")).isInstanceOf(CustomViewNameTranslator.class);
				assertThat(context.getBean("viewNameTranslator"))
					.isInstanceOf(DefaultRequestToViewNameTranslator.class);
			});
	}

	@Test
	void defaultDateFormat() {
		this.contextRunner.run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			Date date = Date.from(ZonedDateTime.of(1988, 6, 25, 20, 30, 0, 0, ZoneId.systemDefault()).toInstant());
			// formatting conversion service should use simple toString()
			assertThat(conversionService.convert(date, String.class)).isEqualTo(date.toString());
		});
	}

	@Test
	void customDateFormat() {
		this.contextRunner.withPropertyValues("spring.mvc.format.date:dd*MM*yyyy").run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			Date date = Date.from(ZonedDateTime.of(1988, 6, 25, 20, 30, 0, 0, ZoneId.systemDefault()).toInstant());
			assertThat(conversionService.convert(date, String.class)).isEqualTo("25*06*1988");
		});
	}

	@Test
	void defaultTimeFormat() {
		this.contextRunner.run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			LocalTime time = LocalTime.of(11, 43, 10);
			assertThat(conversionService.convert(time, String.class))
				.isEqualTo(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time));
		});
	}

	@Test
	void customTimeFormat() {
		this.contextRunner.withPropertyValues("spring.mvc.format.time=HH:mm:ss").run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			LocalTime time = LocalTime.of(11, 43, 10);
			assertThat(conversionService.convert(time, String.class)).isEqualTo("11:43:10");
		});
	}

	@Test
	void defaultDateTimeFormat() {
		this.contextRunner.run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			LocalDateTime dateTime = LocalDateTime.of(2020, 4, 28, 11, 43, 10);
			assertThat(conversionService.convert(dateTime, String.class))
				.isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime));
		});
	}

	@Test
	void customDateTimeTimeFormat() {
		this.contextRunner.withPropertyValues("spring.mvc.format.date-time=yyyy-MM-dd HH:mm:ss").run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			LocalDateTime dateTime = LocalDateTime.of(2020, 4, 28, 11, 43, 10);
			assertThat(conversionService.convert(dateTime, String.class)).isEqualTo("2020-04-28 11:43:10");
		});
	}

	@Test
	void noMessageCodesResolver() {
		this.contextRunner.run(
				(context) -> assertThat(context.getBean(WebMvcAutoConfigurationAdapter.class).getMessageCodesResolver())
					.isNull());
	}

	@Test
	void overrideMessageCodesFormat() {
		this.contextRunner.withPropertyValues("spring.mvc.messageCodesResolverFormat:POSTFIX_ERROR_CODE")
			.run((context) -> assertThat(
					context.getBean(WebMvcAutoConfigurationAdapter.class).getMessageCodesResolver())
				.isNotNull());
	}

	@Test
	void customViewResolver() {
		this.contextRunner.withUserConfiguration(CustomViewResolver.class)
			.run((context) -> assertThat(context.getBean("viewResolver")).isInstanceOf(MyViewResolver.class));
	}

	@Test
	void customContentNegotiatingViewResolver() {
		this.contextRunner.withUserConfiguration(CustomContentNegotiatingViewResolver.class)
			.run((context) -> assertThat(context).getBeanNames(ContentNegotiatingViewResolver.class)
				.containsOnly("myViewResolver"));
	}

	@Test
	void defaultAsyncRequestTimeout() {
		this.contextRunner.run((context) -> assertThat(context.getBean(RequestMappingHandlerAdapter.class))
			.extracting("asyncRequestTimeout")
			.isNull());
	}

	@Test
	void customAsyncRequestTimeout() {
		this.contextRunner.withPropertyValues("spring.mvc.async.request-timeout:12345")
			.run((context) -> assertThat(context.getBean(RequestMappingHandlerAdapter.class))
				.extracting("asyncRequestTimeout")
				.isEqualTo(12345L));
	}

	@Test
	void asyncTaskExecutorWithApplicationTaskExecutor() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncTaskExecutor.class);
				assertThat(context.getBean(RequestMappingHandlerAdapter.class)).extracting("taskExecutor")
					.isSameAs(context.getBean("applicationTaskExecutor"));
			});
	}

	@Test
	void asyncTaskExecutorWithNonMatchApplicationTaskExecutorBean() {
		this.contextRunner.withUserConfiguration(CustomApplicationTaskExecutorConfig.class)
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(AsyncTaskExecutor.class);
				assertThat(context.getBean(RequestMappingHandlerAdapter.class)).extracting("taskExecutor")
					.isNotSameAs(context.getBean("applicationTaskExecutor"));
			});
	}

	@Test
	void asyncTaskExecutorWithMvcConfigurerCanOverrideExecutor() {
		this.contextRunner.withUserConfiguration(CustomAsyncTaskExecutorConfigurer.class)
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(RequestMappingHandlerAdapter.class)).extracting("taskExecutor")
				.isSameAs(context.getBean(CustomAsyncTaskExecutorConfigurer.class).taskExecutor));
	}

	@Test
	void asyncTaskExecutorWithCustomNonApplicationTaskExecutor() {
		this.contextRunner.withUserConfiguration(CustomAsyncTaskExecutorConfig.class)
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncTaskExecutor.class);
				assertThat(context.getBean(RequestMappingHandlerAdapter.class)).extracting("taskExecutor")
					.isNotSameAs(context.getBean("customTaskExecutor"));
			});
	}

	@Test
	void customMediaTypes() {
		this.contextRunner.withPropertyValues("spring.mvc.contentnegotiation.media-types.yaml:text/yaml")
			.run((context) -> assertThat(context.getBean(RequestMappingHandlerAdapter.class))
				.extracting("contentNegotiationManager",
						InstanceOfAssertFactories.type(ContentNegotiationManager.class))
				.satisfies((contentNegotiationManager) -> assertThat(contentNegotiationManager.getAllFileExtensions())
					.contains("yaml")));
	}

	@Test
	void customDefaultContentTypes() {
		this.contextRunner
			.withPropertyValues("spring.mvc.contentnegotiation.default-content-types:application/json,application/xml")
			.run((context) -> assertThat(context.getBean(RequestMappingHandlerAdapter.class))
				.extracting("contentNegotiationManager",
						InstanceOfAssertFactories.type(ContentNegotiationManager.class))
				.satisfies((contentNegotiationManager) -> assertThat(
						contentNegotiationManager.getStrategy(FixedContentNegotiationStrategy.class).getContentTypes())
					.containsExactly(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)));
	}

	@Test
	void formContentFilterIsAutoConfigured() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(OrderedFormContentFilter.class));
	}

	@Test
	void formContentFilterCanBeOverridden() {
		this.contextRunner.withUserConfiguration(CustomFormContentFilter.class).run((context) -> {
			assertThat(context).doesNotHaveBean(OrderedFormContentFilter.class);
			assertThat(context).hasSingleBean(FormContentFilter.class);
		});
	}

	@Test
	void formContentFilterCanBeDisabled() {
		this.contextRunner.withPropertyValues("spring.mvc.formcontent.filter.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FormContentFilter.class));
	}

	@Test
	void hiddenHttpMethodFilterCanBeEnabled() {
		this.contextRunner.withPropertyValues("spring.mvc.hiddenmethod.filter.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(HiddenHttpMethodFilter.class));
	}

	@Test
	void hiddenHttpMethodFilterDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HiddenHttpMethodFilter.class));
	}

	@Test
	void customConfigurableWebBindingInitializer() {
		this.contextRunner.withUserConfiguration(CustomConfigurableWebBindingInitializer.class)
			.run((context) -> assertThat(context.getBean(RequestMappingHandlerAdapter.class).getWebBindingInitializer())
				.isInstanceOf(CustomWebBindingInitializer.class));
	}

	@Test
	void customRequestMappingHandlerMapping() {
		this.contextRunner.withUserConfiguration(CustomRequestMappingHandlerMapping.class).run((context) -> {
			assertThat(context).getBean(RequestMappingHandlerMapping.class)
				.isInstanceOf(MyRequestMappingHandlerMapping.class);
			assertThat(context.getBean(CustomRequestMappingHandlerMapping.class).handlerMappings).isOne();
		});
	}

	@Test
	void customRequestMappingHandlerAdapter() {
		this.contextRunner.withUserConfiguration(CustomRequestMappingHandlerAdapter.class).run((context) -> {
			assertThat(context).getBean(RequestMappingHandlerAdapter.class)
				.isInstanceOf(MyRequestMappingHandlerAdapter.class);
			assertThat(context.getBean(CustomRequestMappingHandlerAdapter.class).handlerAdapters).isOne();
		});
	}

	@Test
	void customExceptionHandlerExceptionResolver() {
		this.contextRunner.withUserConfiguration(CustomExceptionHandlerExceptionResolver.class)
			.run((context) -> assertThat(
					context.getBean(CustomExceptionHandlerExceptionResolver.class).exceptionResolvers)
				.isOne());
	}

	@Test
	void multipleWebMvcRegistrations() {
		this.contextRunner.withUserConfiguration(MultipleWebMvcRegistrations.class).run((context) -> {
			assertThat(context.getBean(RequestMappingHandlerMapping.class))
				.isNotInstanceOf(MyRequestMappingHandlerMapping.class);
			assertThat(context.getBean(RequestMappingHandlerAdapter.class))
				.isNotInstanceOf(MyRequestMappingHandlerAdapter.class);
		});
	}

	@Test
	void defaultLogResolvedException() {
		this.contextRunner.run(assertExceptionResolverWarnLoggers((logger) -> assertThat(logger).isNull()));
	}

	@Test
	void customLogResolvedException() {
		this.contextRunner.withPropertyValues("spring.mvc.log-resolved-exception:true")
			.run(assertExceptionResolverWarnLoggers((logger) -> assertThat(logger).isNotNull()));
	}

	private ContextConsumer<AssertableWebApplicationContext> assertExceptionResolverWarnLoggers(
			Consumer<Object> consumer) {
		return (context) -> {
			HandlerExceptionResolver resolver = context.getBean(HandlerExceptionResolver.class);
			assertThat(resolver).isInstanceOf(HandlerExceptionResolverComposite.class);
			List<HandlerExceptionResolver> delegates = ((HandlerExceptionResolverComposite) resolver)
				.getExceptionResolvers();
			for (HandlerExceptionResolver delegate : delegates) {
				if (delegate instanceof AbstractHandlerExceptionResolver
						&& !(delegate instanceof DefaultHandlerExceptionResolver)) {
					consumer.accept(ReflectionTestUtils.getField(delegate, "warnLogger"));
				}
			}
		};
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void welcomePageHandlerMappingIsAutoConfigured() {
		this.contextRunner.withPropertyValues("spring.web.resources.static-locations:classpath:/welcome-page/")
			.run((context) -> {
				assertThat(context).hasSingleBean(WelcomePageHandlerMapping.class);
				assertThat(context.getBean(WelcomePageHandlerMapping.class).getRootHandler()).isNotNull();
				assertThat(context.getBean(WelcomePageNotAcceptableHandlerMapping.class).getRootHandler()).isNotNull();
			});
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void welcomePageHandlerIncludesCorsConfiguration() {
		this.contextRunner.withPropertyValues("spring.web.resources.static-locations:classpath:/welcome-page/")
			.withUserConfiguration(CorsConfigurer.class)
			.run((context) -> {
				WelcomePageHandlerMapping bean = context.getBean(WelcomePageHandlerMapping.class);
				UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) bean
					.getCorsConfigurationSource();
				assertThat(source.getCorsConfigurations()).containsKey("/**");
			});
	}

	@Test
	void validatorWhenNoValidatorShouldUseDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
		});
	}

	@Test
	void validatorWhenNoCustomizationShouldUseAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).getBeanNames(jakarta.validation.Validator.class).containsOnly("defaultValidator");
				assertThat(context).getBeanNames(Validator.class).containsOnly("defaultValidator", "mvcValidator");
				Validator validator = context.getBean("mvcValidator", Validator.class);
				assertThat(validator).isInstanceOf(ValidatorAdapter.class);
				Object defaultValidator = context.getBean("defaultValidator");
				assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
				// Primary Spring validator is the one used by MVC behind the scenes
				assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
			});
	}

	@Test
	void validatorWithConfigurerAloneShouldUseSpringValidator() {
		this.contextRunner.withUserConfiguration(MvcValidator.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
			Validator expectedValidator = context.getBean(MvcValidator.class).validator;
			assertThat(context.getBean("mvcValidator")).isSameAs(expectedValidator);
			assertThat(context.getBean(RequestMappingHandlerAdapter.class).getWebBindingInitializer())
				.hasFieldOrPropertyWithValue("validator", expectedValidator);
		});
	}

	@Test
	void validatorWithConfigurerShouldUseSpringValidator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(MvcValidator.class)
			.run((context) -> {
				assertThat(context).getBeanNames(jakarta.validation.Validator.class).containsOnly("defaultValidator");
				assertThat(context).getBeanNames(Validator.class).containsOnly("defaultValidator", "mvcValidator");
				Validator expectedValidator = context.getBean(MvcValidator.class).validator;
				assertThat(context.getBean("mvcValidator")).isSameAs(expectedValidator);
				assertThat(context.getBean(RequestMappingHandlerAdapter.class).getWebBindingInitializer())
					.hasFieldOrPropertyWithValue("validator", expectedValidator);
			});
	}

	@Test
	void validatorWithConfigurerDoesNotExposeJsr303() {
		this.contextRunner.withUserConfiguration(MvcJsr303Validator.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
			Validator validator = context.getBean("mvcValidator", Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			assertThat(((ValidatorAdapter) validator).getTarget())
				.isSameAs(context.getBean(MvcJsr303Validator.class).validator);
		});
	}

	@Test
	void validatorWithConfigurerTakesPrecedence() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(MvcValidator.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ValidatorFactory.class);
				assertThat(context).hasSingleBean(jakarta.validation.Validator.class);
				assertThat(context).getBeanNames(Validator.class).containsOnly("defaultValidator", "mvcValidator");
				assertThat(context.getBean("mvcValidator")).isSameAs(context.getBean(MvcValidator.class).validator);
				// Primary Spring validator is the auto-configured one as the MVC one
				// has been customized through a WebMvcConfigurer
				assertThat(context.getBean(Validator.class)).isEqualTo(context.getBean("defaultValidator"));
			});
	}

	@Test
	void validatorWithCustomSpringValidatorIgnored() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(CustomSpringValidator.class)
			.run((context) -> {
				assertThat(context).getBeanNames(jakarta.validation.Validator.class).containsOnly("defaultValidator");
				assertThat(context).getBeanNames(Validator.class)
					.containsOnly("customSpringValidator", "defaultValidator", "mvcValidator");
				Validator validator = context.getBean("mvcValidator", Validator.class);
				assertThat(validator).isInstanceOf(ValidatorAdapter.class);
				Object defaultValidator = context.getBean("defaultValidator");
				assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
				// Primary Spring validator is the one used by MVC behind the scenes
				assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
			});
	}

	@Test
	void validatorWithCustomJsr303ValidatorExposedAsSpringValidator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(CustomJsr303Validator.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ValidatorFactory.class);
				assertThat(context).getBeanNames(jakarta.validation.Validator.class)
					.containsOnly("customJsr303Validator");
				assertThat(context).getBeanNames(Validator.class).containsOnly("mvcValidator");
				Validator validator = context.getBean(Validator.class);
				assertThat(validator).isInstanceOf(ValidatorAdapter.class);
				Validator target = ((ValidatorAdapter) validator).getTarget();
				assertThat(target).extracting("targetValidator").isSameAs(context.getBean("customJsr303Validator"));
			});
	}

	@Test
	void httpMessageConverterThatUsesConversionServiceDoesNotCreateACycle() {
		this.contextRunner.withUserConfiguration(CustomHttpMessageConverter.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void cachePeriod() {
		this.contextRunner.withPropertyValues("spring.web.resources.cache.period:5").run((context) -> {
			assertResourceHttpRequestHandler((context), (handler) -> {
				assertThat(handler.getCacheSeconds()).isEqualTo(5);
				assertThat(handler.getCacheControl()).isNull();
			});
		});
	}

	@Test
	void cacheControl() {
		this.contextRunner
			.withPropertyValues("spring.web.resources.cache.cachecontrol.max-age:5",
					"spring.web.resources.cache.cachecontrol.proxy-revalidate:true")
			.run((context) -> assertResourceHttpRequestHandler(context, (handler) -> {
				assertThat(handler.getCacheSeconds()).isEqualTo(-1);
				assertThat(handler.getCacheControl()).usingRecursiveComparison()
					.isEqualTo(CacheControl.maxAge(5, TimeUnit.SECONDS).proxyRevalidate());
			}));
	}

	@Test
	void usePathPatternParser() {
		this.contextRunner.withPropertyValues("spring.mvc.pathmatch.matching-strategy:path_pattern_parser")
			.run((context) -> {
				RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
				assertThat(handlerMapping.usesPathPatterns()).isTrue();
			});
	}

	@Test
	void queryParameterContentNegotiation() {
		this.contextRunner.withPropertyValues("spring.mvc.contentnegotiation.favor-parameter:true").run((context) -> {
			RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
			ContentNegotiationManager contentNegotiationManager = handlerMapping.getContentNegotiationManager();
			assertThat(contentNegotiationManager.getStrategies())
				.hasAtLeastOneElementOfType(ParameterContentNegotiationStrategy.class);
		});
	}

	@Test
	void requestContextFilterIsAutoConfigured() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RequestContextFilter.class));
	}

	@Test
	void whenUserDefinesARequestContextFilterTheAutoConfiguredRegistrationBacksOff() {
		this.contextRunner.withUserConfiguration(RequestContextFilterConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RequestContextFilter.class);
			assertThat(context).hasBean("customRequestContextFilter");
		});
	}

	@Test
	void whenUserDefinesARequestContextFilterRegistrationTheAutoConfiguredFilterBacksOff() {
		this.contextRunner.withUserConfiguration(RequestContextFilterRegistrationConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			assertThat(context).hasBean("customRequestContextFilterRegistration");
			assertThat(context).doesNotHaveBean(RequestContextFilter.class);
		});
	}

	@Test
	void customPrinterAndParserShouldBeRegisteredAsConverters() {
		this.contextRunner.withUserConfiguration(ParserConfiguration.class, PrinterConfiguration.class)
			.run((context) -> {
				ConversionService service = context.getBean(ConversionService.class);
				assertThat(service.convert(new Example("spring", new Date()), String.class)).isEqualTo("spring");
				assertThat(service.convert("boot", Example.class)).extracting(Example::getName).isEqualTo("boot");
			});
	}

	@Test
	void urlPathHelperUsesFullPathByDefaultWhenAntPathMatchingIsUsed() {
		this.contextRunner.withPropertyValues("spring.mvc.pathmatch.matching-strategy:ant-path-matcher")
			.run((context) -> {
				UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
				assertThat(urlPathHelper).extracting("alwaysUseFullPath").isEqualTo(true);
			});
	}

	@Test
	void urlPathHelperDoesNotUseFullPathWithServletMapping() {
		this.contextRunner.withPropertyValues("spring.mvc.pathmatch.matching-strategy:ant-path-matcher")
			.withPropertyValues("spring.mvc.servlet.path=/test/")
			.run((context) -> {
				UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
				assertThat(urlPathHelper).extracting("alwaysUseFullPath").isEqualTo(false);
			});
	}

	@Test
	void urlPathHelperDoesNotUseFullPathWithAdditionalDispatcherServlet() {
		this.contextRunner.withUserConfiguration(AdditionalDispatcherServletConfiguration.class).run((context) -> {
			UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
			assertThat(urlPathHelper).extracting("alwaysUseFullPath").isEqualTo(false);
		});
	}

	@Test
	void urlPathHelperDoesNotUseFullPathWithAdditionalUntypedDispatcherServlet() {
		this.contextRunner.withUserConfiguration(AdditionalUntypedDispatcherServletConfiguration.class)
			.run((context) -> {
				UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
				assertThat(urlPathHelper).extracting("alwaysUseFullPath").isEqualTo(false);
			});
	}

	@Test
	void lastModifiedNotUsedIfDisabled() {
		this.contextRunner.withPropertyValues("spring.web.resources.cache.use-last-modified=false")
			.run((context) -> assertResourceHttpRequestHandler(context,
					(handler) -> assertThat(handler.isUseLastModified()).isFalse()));
	}

	@Test // gh-25743
	void addResourceHandlersAppliesToChildAndParentContext() {
		try (AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext()) {
			context.register(WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
					ResourceHandlersWithChildAndParentContextConfiguration.class);
			context.refresh();
			SimpleUrlHandlerMapping resourceHandlerMapping = context.getBean("resourceHandlerMapping",
					SimpleUrlHandlerMapping.class);
			DispatcherServlet extraDispatcherServlet = context.getBean("extraDispatcherServlet",
					DispatcherServlet.class);
			SimpleUrlHandlerMapping extraResourceHandlerMapping = extraDispatcherServlet.getWebApplicationContext()
				.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(resourceHandlerMapping).isNotSameAs(extraResourceHandlerMapping);
			assertThat(resourceHandlerMapping.getUrlMap()).containsKey("/**");
			assertThat(extraResourceHandlerMapping.getUrlMap()).containsKey("/**");
		}
	}

	@Test
	void problemDetailsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ProblemDetailsExceptionHandler.class));
	}

	@Test
	void problemDetailsEnabledAddsExceptionHandler() {
		this.contextRunner.withPropertyValues("spring.mvc.problemdetails.enabled:true")
			.run((context) -> assertThat(context).hasSingleBean(ProblemDetailsExceptionHandler.class));
	}

	@Test
	void problemDetailsExceptionHandlerDoesNotPreventProxying() {
		this.contextRunner.withUserConfiguration(AopConfiguration.class)
			.withBean(ExceptionHandlerInterceptor.class)
			.withPropertyValues("spring.mvc.problemdetails.enabled:true")
			.run((context) -> assertThat(context).getBean(ProblemDetailsExceptionHandler.class)
				.matches(AopUtils::isCglibProxy));
	}

	@Test
	void problemDetailsBacksOffWhenExceptionHandler() {
		this.contextRunner.withPropertyValues("spring.mvc.problemdetails.enabled:true")
			.withUserConfiguration(CustomExceptionHandlerConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ProblemDetailsExceptionHandler.class)
				.hasSingleBean(CustomExceptionHandler.class));
	}

	@Test
	void problemDetailsExceptionHandlerIsOrderedAt0() {
		this.contextRunner.withPropertyValues("spring.mvc.problemdetails.enabled:true")
			.withUserConfiguration(OrderedControllerAdviceBeansConfiguration.class)
			.run((context) -> assertThat(
					ControllerAdviceBean.findAnnotatedBeans(context).stream().map(ControllerAdviceBean::getBeanType))
				.asInstanceOf(InstanceOfAssertFactories.list(Class.class))
				.containsExactly(HighestOrderedControllerAdvice.class, ProblemDetailsExceptionHandler.class,
						OrderedControllerAdviceBeansConfiguration.LowestOrderedControllerAdvice.class));
	}

	@Test
	void apiVersionPropertiesAreApplied() {
		this.contextRunner
			.withPropertyValues("spring.mvc.apiversion.use.header=version", "spring.mvc.apiversion.required=true",
					"spring.mvc.apiversion.supported=123,456", "spring.mvc.apiversion.detect-supported=false")
			.run((context) -> {
				DefaultApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy",
						DefaultApiVersionStrategy.class);
				assertThatExceptionOfType(MissingApiVersionException.class)
					.isThrownBy(() -> versionStrategy.validateVersion(null, new MockHttpServletRequest()));
				assertThatExceptionOfType(InvalidApiVersionException.class).isThrownBy(() -> versionStrategy
					.validateVersion(versionStrategy.parseVersion("789"), new MockHttpServletRequest()));
				assertThat(versionStrategy.detectSupportedVersions()).isFalse();
			});
	}

	@Test
	void apiVersionDefaultVersionPropertyIsApplied() {
		this.contextRunner
			.withPropertyValues("spring.mvc.apiversion.use.header=version", "spring.mvc.apiversion.default=1.0.0")
			.run((context) -> {
				DefaultApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy",
						DefaultApiVersionStrategy.class);
				versionStrategy.addSupportedVersion("1.0.0");
				Comparable<?> version = versionStrategy.parseVersion("1.0.0");
				assertThat(versionStrategy.getDefaultVersion()).isEqualTo(version);
				versionStrategy.validateVersion(version, new MockHttpServletRequest());
				versionStrategy.validateVersion(null, new MockHttpServletRequest());
			});
	}

	@Test
	void apiVersionUseHeaderPropertyIsApplied() {
		this.contextRunner.withPropertyValues("spring.mvc.apiversion.use.header=hv").run((context) -> {
			ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addHeader("hv", "123");
			assertThat(versionStrategy.resolveVersion(request)).isEqualTo("123");
		});
	}

	@Test
	void apiVersionUseQueryParameterPropertyIsApplied() {
		this.contextRunner.withPropertyValues("spring.mvc.apiversion.use.query-parameter=rpv").run((context) -> {
			ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setQueryString("rpv=123");
			assertThat(versionStrategy.resolveVersion(request)).isEqualTo("123");
		});
	}

	@Test
	void apiVersionUsePathSegmentPropertyIsApplied() {
		this.contextRunner.withPropertyValues("spring.mvc.apiversion.use.path-segment=1").run((context) -> {
			ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "https://example.com/test/123");
			ServletRequestPathUtils.setParsedRequestPath(RequestPath.parse("/test/123", "/"), request);
			assertThat(versionStrategy.resolveVersion(request)).isEqualTo("123");
		});
	}

	@Test
	void apiVersionUseMediaTypeParameterPropertyIsApplied() {
		this.contextRunner.withPropertyValues("spring.mvc.apiversion.use.media-type-parameter[application/json]=mtpv")
			.run((context) -> {
				ApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy", ApiVersionStrategy.class);
				MockHttpServletRequest request = new MockHttpServletRequest();
				request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json;mtpv=123");
				assertThat(versionStrategy.resolveVersion(request)).isEqualTo("123");
			});
	}

	@Test
	void apiVersionBeansAreInjected() {
		this.contextRunner.withUserConfiguration(ApiVersionConfiguration.class).run((context) -> {
			DefaultApiVersionStrategy versionStrategy = context.getBean("mvcApiVersionStrategy",
					DefaultApiVersionStrategy.class);
			assertThat(versionStrategy).extracting("versionResolvers")
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsExactly(context.getBean(ApiVersionResolver.class));
			assertThat(versionStrategy).extracting("deprecationHandler")
				.isEqualTo(context.getBean(ApiVersionDeprecationHandler.class));
			assertThat(versionStrategy).extracting("versionParser").isEqualTo(context.getBean(ApiVersionParser.class));
			assertThat(versionStrategy).extracting("supportedVersionPredicate")
				.isEqualTo(context.getBean("supportedVersionPredicate"));
		});
	}

	private void assertResourceHttpRequestHandler(AssertableWebApplicationContext context,
			Consumer<ResourceHttpRequestHandler> handlerConsumer) {
		Map<String, Object> handlerMap = getHandlerMap(context.getBean("resourceHandlerMapping", HandlerMapping.class));
		assertThat(handlerMap).hasSize(2);
		for (Object handler : handlerMap.values()) {
			if (handler instanceof ResourceHttpRequestHandler resourceHandler) {
				handlerConsumer.accept(resourceHandler);
			}
		}
	}

	protected Map<String, List<Resource>> getResourceMappingLocations(ApplicationContext context) {
		Object bean = context.getBean("resourceHandlerMapping");
		if (bean instanceof HandlerMapping handlerMapping) {
			return getMappingLocations(context, handlerMapping);
		}
		assertThat(bean).hasToString("null");
		return Collections.emptyMap();
	}

	protected List<ResourceResolver> getResourceResolvers(ApplicationContext context, String mapping) {
		ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) context
			.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class)
			.getHandlerMap()
			.get(mapping);
		return resourceHandler.getResourceResolvers();
	}

	protected List<ResourceTransformer> getResourceTransformers(ApplicationContext context, String mapping) {
		SimpleUrlHandlerMapping handler = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
		ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler.getHandlerMap().get(mapping);
		return resourceHandler.getResourceTransformers();
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<Resource>> getMappingLocations(ApplicationContext context, HandlerMapping mapping) {
		Map<String, List<Resource>> mappingLocations = new LinkedHashMap<>();
		getHandlerMap(mapping).forEach((key, value) -> {
			List<String> locationValues = (List<String>) ReflectionTestUtils.getField(value, "locationValues");
			List<Resource> locationResources = (List<Resource>) ReflectionTestUtils.getField(value,
					"locationResources");
			List<Resource> resources = new ArrayList<>();
			for (String locationValue : locationValues) {
				resources.add(context.getResource(locationValue));
			}
			resources.addAll(locationResources);
			mappingLocations.put(key, resources);
		});
		return mappingLocations;
	}

	protected Map<String, Object> getHandlerMap(HandlerMapping mapping) {
		if (mapping instanceof SimpleUrlHandlerMapping handlerMapping) {
			return handlerMapping.getHandlerMap();
		}
		return Collections.emptyMap();
	}

	@Configuration(proxyBeanMethods = false)
	static class ViewConfig {

		@Bean
		View jsonView() {
			return new AbstractView() {

				@Override
				protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
						HttpServletResponse response) throws Exception {
					response.getOutputStream().write("Hello World".getBytes());
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WebJars implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/foo/");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AllResources implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/**").addResourceLocations("classpath:/foo/");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return webServerFactory;
		}

		@Bean
		static WebServerFactoryCustomizerBeanPostProcessor servletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomViewResolver {

		@Bean
		ViewResolver viewResolver() {
			return new MyViewResolver();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomContentNegotiatingViewResolver {

		@Bean
		ContentNegotiatingViewResolver myViewResolver() {
			return new ContentNegotiatingViewResolver();
		}

	}

	static class MyViewResolver implements ViewResolver {

		@Override
		public View resolveViewName(String viewName, Locale locale) {
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfigurableWebBindingInitializer {

		@Bean
		ConfigurableWebBindingInitializer customConfigurableWebBindingInitializer() {
			return new CustomWebBindingInitializer();
		}

	}

	static class CustomWebBindingInitializer extends ConfigurableWebBindingInitializer {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFormContentFilter {

		@Bean
		FormContentFilter customFormContentFilter() {
			return new FormContentFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRequestMappingHandlerMapping {

		private int handlerMappings;

		@Bean
		WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
			return new WebMvcRegistrations() {

				@Override
				public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
					CustomRequestMappingHandlerMapping.this.handlerMappings++;
					return new MyRequestMappingHandlerMapping();
				}

			};
		}

	}

	static class MyRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRequestMappingHandlerAdapter {

		private int handlerAdapters = 0;

		@Bean
		WebMvcRegistrations webMvcRegistrationsHandlerAdapter() {
			return new WebMvcRegistrations() {

				@Override
				public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
					CustomRequestMappingHandlerAdapter.this.handlerAdapters++;
					return new MyRequestMappingHandlerAdapter();
				}

			};
		}

	}

	static class MyRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomExceptionHandlerExceptionResolver {

		private int exceptionResolvers = 0;

		@Bean
		WebMvcRegistrations webMvcRegistrationsExceptionResolver() {
			return new WebMvcRegistrations() {

				@Override
				public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
					CustomExceptionHandlerExceptionResolver.this.exceptionResolvers++;
					return new MyExceptionHandlerExceptionResolver();
				}

			};
		}

	}

	static class MyExceptionHandlerExceptionResolver extends ExceptionHandlerExceptionResolver {

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ CustomRequestMappingHandlerMapping.class, CustomRequestMappingHandlerAdapter.class })
	static class MultipleWebMvcRegistrations {

	}

	@Configuration(proxyBeanMethods = false)
	static class MvcValidator implements WebMvcConfigurer {

		private final Validator validator = mock(Validator.class);

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MvcJsr303Validator implements WebMvcConfigurer {

		private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJsr303Validator {

		@Bean
		jakarta.validation.Validator customJsr303Validator() {
			return mock(jakarta.validation.Validator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSpringValidator {

		@Bean
		Validator customSpringValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHttpMessageConverter {

		@Bean
		HttpMessageConverter<?> customHttpMessageConverter(ConversionService conversionService) {
			return mock(HttpMessageConverter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomApplicationTaskExecutorConfig {

		@Bean
		Executor applicationTaskExecutor() {
			return mock(Executor.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAsyncTaskExecutorConfig {

		@Bean
		AsyncTaskExecutor customTaskExecutor() {
			return mock(AsyncTaskExecutor.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAsyncTaskExecutorConfigurer implements WebMvcConfigurer {

		private final AsyncTaskExecutor taskExecutor = mock(AsyncTaskExecutor.class);

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			configurer.setTaskExecutor(this.taskExecutor);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextFilterConfiguration {

		@Bean
		RequestContextFilter customRequestContextFilter() {
			return new RequestContextFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextFilterRegistrationConfiguration {

		@Bean
		FilterRegistrationBean<RequestContextFilter> customRequestContextFilterRegistration() {
			return new FilterRegistrationBean<>(new RequestContextFilter());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PrinterConfiguration {

		@Bean
		Printer<Example> examplePrinter() {
			return new ExamplePrinter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParserConfiguration {

		@Bean
		Parser<Example> exampleParser() {
			return new ExampleParser();
		}

	}

	static final class Example {

		private final String name;

		private Example(String name, Date date) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

	static class ExamplePrinter implements Printer<Example> {

		@Override
		public String print(Example example, Locale locale) {
			return example.getName();
		}

	}

	static class ExampleParser implements Parser<Example> {

		@Override
		public Example parse(String source, Locale locale) {
			return new Example(source, new Date());
		}

	}

	@Configuration
	static class CorsConfigurer implements WebMvcConfigurer {

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/**").allowedMethods("GET");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AdditionalDispatcherServletConfiguration {

		@Bean
		ServletRegistrationBean<DispatcherServlet> additionalDispatcherServlet() {
			return new ServletRegistrationBean<>(new DispatcherServlet());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AdditionalUntypedDispatcherServletConfiguration {

		@Bean
		ServletRegistrationBean<?> additionalDispatcherServlet() {
			return new ServletRegistrationBean<>(new DispatcherServlet());
		}

	}

	static class CustomLocaleResolver implements LocaleResolver {

		@Override
		public Locale resolveLocale(HttpServletRequest request) {
			return Locale.ENGLISH;
		}

		@Override
		public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		}

	}

	static class CustomFlashMapManager extends AbstractFlashMapManager {

		@Override
		protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
			return null;
		}

		@Override
		protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request,
				HttpServletResponse response) {

		}

	}

	static class CustomViewNameTranslator implements RequestToViewNameTranslator {

		@Override
		public String getViewName(HttpServletRequest requestAttributes) {
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceHandlersWithChildAndParentContextConfiguration {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		ServletRegistrationBean<?> additionalDispatcherServlet(DispatcherServlet extraDispatcherServlet) {
			ServletRegistrationBean<?> registration = new ServletRegistrationBean<>(extraDispatcherServlet, "/extra/*");
			registration.setName("additionalDispatcherServlet");
			registration.setLoadOnStartup(1);
			return registration;
		}

		@Bean
		private DispatcherServlet extraDispatcherServlet() {
			DispatcherServlet dispatcherServlet = new DispatcherServlet();
			AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
			applicationContext.register(ResourceHandlersWithChildAndParentContextChildConfiguration.class);
			dispatcherServlet.setApplicationContext(applicationContext);
			return dispatcherServlet;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class ResourceHandlersWithChildAndParentContextChildConfiguration {

		@Bean
		WebMvcConfigurer myConfigurer() {
			return new WebMvcConfigurer() {

				@Override
				public void addResourceHandlers(ResourceHandlerRegistry registry) {
					registry.addResourceHandler("/testtesttest");
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomExceptionHandlerConfiguration {

		@Bean
		CustomExceptionHandler customExceptionHandler() {
			return new CustomExceptionHandler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ LowestOrderedControllerAdvice.class, HighestOrderedControllerAdvice.class })
	static class OrderedControllerAdviceBeansConfiguration {

		@ControllerAdvice
		@Order
		static class LowestOrderedControllerAdvice {

		}

		@ControllerAdvice
		@Order(Ordered.HIGHEST_PRECEDENCE)
		static class HighestOrderedControllerAdvice {

		}

	}

	@ControllerAdvice
	static class CustomExceptionHandler extends ResponseEntityExceptionHandler {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class AopConfiguration {

	}

	@Aspect
	static class ExceptionHandlerInterceptor {

		@AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ExceptionHandler)",
				returning = "returnValue")
		void exceptionHandlerIntercept(JoinPoint joinPoint, Object returnValue) {
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ApiVersionConfiguration {

		@Bean
		ApiVersionResolver apiVersionResolver() {
			return (request) -> "latest";
		}

		@Bean
		ApiVersionDeprecationHandler apiVersionDeprecationHandler(ApiVersionParser<?> apiVersionParser) {
			return new StandardApiVersionDeprecationHandler(apiVersionParser);
		}

		@Bean
		ApiVersionParser<String> apiVersionParser() {
			return (version) -> String.valueOf(version);
		}

		@Bean
		Predicate<Comparable<?>> supportedVersionPredicate() {
			return (comparable) -> true;
		}

		@Bean
		ApiVersionCustomizer apiVersionCustomizer(
				@Qualifier("supportedVersionPredicate") Predicate<Comparable<?>> supportedVersionPredicate) {
			return (configurer) -> configurer.setSupportedVersionPredicate(supportedVersionPredicate);
		}

	}

}
