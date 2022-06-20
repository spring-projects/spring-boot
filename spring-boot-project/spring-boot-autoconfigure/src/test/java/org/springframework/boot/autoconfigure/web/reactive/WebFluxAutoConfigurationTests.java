/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.validation.ValidatorFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.WebFluxConfig;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.CachingResourceResolver;
import org.springframework.web.reactive.resource.CachingResourceTransformer;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.util.pattern.PathPattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebFluxAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 */
class WebFluxAutoConfigurationTests {

	private static final MockReactiveWebServerFactory mockReactiveWebServerFactory = new MockReactiveWebServerFactory();

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(WebFluxAutoConfiguration.class, WebSessionIdResolverAutoConfiguration.class))
			.withUserConfiguration(Config.class);

	@Test
	void shouldNotProcessIfExistingWebReactiveConfiguration() {
		this.contextRunner.withUserConfiguration(WebFluxConfigurationSupport.class).run((context) -> {
			assertThat(context).getBeans(RequestMappingHandlerMapping.class).hasSize(1);
			assertThat(context).getBeans(RequestMappingHandlerAdapter.class).hasSize(1);
		});
	}

	@Test
	void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(RequestMappingHandlerMapping.class).hasSize(1);
			assertThat(context).getBeans(RequestMappingHandlerAdapter.class).hasSize(1);
			assertThat(context).getBeans(RequestedContentTypeResolver.class).hasSize(1);
			assertThat(context).getBeans(RouterFunctionMapping.class).hasSize(1);
			assertThat(context.getBean(WebHttpHandlerBuilder.WEB_SESSION_MANAGER_BEAN_NAME, WebSessionManager.class))
					.isNotNull();
			assertThat(context.getBean("resourceHandlerMapping", HandlerMapping.class)).isNotNull();
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldRegisterCustomHandlerMethodArgumentResolver() {
		this.contextRunner.withUserConfiguration(CustomArgumentResolvers.class).run((context) -> {
			RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
			List<HandlerMethodArgumentResolver> customResolvers = (List<HandlerMethodArgumentResolver>) ReflectionTestUtils
					.getField(adapter.getArgumentResolverConfigurer(), "customResolvers");
			assertThat(customResolvers).contains(context.getBean("firstResolver", HandlerMethodArgumentResolver.class),
					context.getBean("secondResolver", HandlerMethodArgumentResolver.class));
		});
	}

	@Test
	void shouldCustomizeCodecs() {
		this.contextRunner.withUserConfiguration(CustomCodecCustomizers.class).run((context) -> {
			CodecCustomizer codecCustomizer = context.getBean("firstCodecCustomizer", CodecCustomizer.class);
			assertThat(codecCustomizer).isNotNull();
			then(codecCustomizer).should().customize(any(ServerCodecConfigurer.class));
		});
	}

	@Test
	void shouldRegisterResourceHandlerMapping() {
		this.contextRunner.run((context) -> {
			SimpleUrlHandlerMapping hm = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
			assertThat(staticHandler.getLocations()).hasSize(4);
			assertThat(hm.getUrlMap().get("/webjars/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler webjarsHandler = (ResourceWebHandler) hm.getUrlMap().get("/webjars/**");
			assertThat(webjarsHandler.getLocations()).hasSize(1);
			assertThat(webjarsHandler.getLocations().get(0))
					.isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
		});
	}

	@Test
	void shouldMapResourcesToCustomPath() {
		this.contextRunner.withPropertyValues("spring.webflux.static-path-pattern:/static/**").run((context) -> {
			SimpleUrlHandlerMapping hm = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(hm.getUrlMap().get("/static/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/static/**");
			assertThat(staticHandler).extracting("locationValues").asList().hasSize(4);
		});
	}

	@Test
	void shouldNotMapResourcesWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.web.resources.add-mappings:false")
				.run((context) -> assertThat(context.getBean("resourceHandlerMapping"))
						.isNotInstanceOf(SimpleUrlHandlerMapping.class));
	}

	@Test
	void resourceHandlerChainEnabled() {
		this.contextRunner.withPropertyValues("spring.web.resources.chain.enabled:true").run((context) -> {
			SimpleUrlHandlerMapping hm = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
			assertThat(staticHandler.getResourceResolvers()).extractingResultOf("getClass")
					.containsOnly(CachingResourceResolver.class, PathResourceResolver.class);
			assertThat(staticHandler.getResourceTransformers()).extractingResultOf("getClass")
					.containsOnly(CachingResourceTransformer.class);
		});
	}

	@Test
	void shouldRegisterViewResolvers() {
		this.contextRunner.withUserConfiguration(ViewResolvers.class).run((context) -> {
			ViewResolutionResultHandler resultHandler = context.getBean(ViewResolutionResultHandler.class);
			assertThat(resultHandler.getViewResolvers()).containsExactly(
					context.getBean("aViewResolver", ViewResolver.class),
					context.getBean("anotherViewResolver", ViewResolver.class));
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
		this.contextRunner.withPropertyValues("spring.webflux.format.date:dd*MM*yyyy").run((context) -> {
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
		this.contextRunner.withPropertyValues("spring.webflux.format.time=HH:mm:ss").run((context) -> {
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
		this.contextRunner.withPropertyValues("spring.webflux.format.date-time=yyyy-MM-dd HH:mm:ss").run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			LocalDateTime dateTime = LocalDateTime.of(2020, 4, 28, 11, 43, 10);
			assertThat(conversionService.convert(dateTime, String.class)).isEqualTo("2020-04-28 11:43:10");
		});
	}

	@Test
	void validatorWhenNoValidatorShouldUseDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsExactly("webFluxValidator");
		});
	}

	@Test
	void validatorWhenNoCustomizationShouldUseAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).getBeanNames(jakarta.validation.Validator.class)
							.containsExactly("defaultValidator");
					assertThat(context).getBeanNames(Validator.class).containsExactlyInAnyOrder("defaultValidator",
							"webFluxValidator");
					Validator validator = context.getBean("webFluxValidator", Validator.class);
					assertThat(validator).isInstanceOf(ValidatorAdapter.class);
					Object defaultValidator = context.getBean("defaultValidator");
					assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
					// Primary Spring validator is the one used by WebFlux behind the
					// scenes
					assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
				});
	}

	@Test
	void validatorWithConfigurerShouldUseSpringValidator() {
		this.contextRunner.withUserConfiguration(ValidatorWebFluxConfigurer.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("webFluxValidator");
			assertThat(context.getBean("webFluxValidator"))
					.isSameAs(context.getBean(ValidatorWebFluxConfigurer.class).validator);
		});
	}

	@Test
	void validatorWithConfigurerDoesNotExposeJsr303() {
		this.contextRunner.withUserConfiguration(ValidatorJsr303WebFluxConfigurer.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(jakarta.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("webFluxValidator");
			Validator validator = context.getBean("webFluxValidator", Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			assertThat(((ValidatorAdapter) validator).getTarget())
					.isSameAs(context.getBean(ValidatorJsr303WebFluxConfigurer.class).validator);
		});
	}

	@Test
	void validationCustomConfigurerTakesPrecedence() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.withUserConfiguration(ValidatorWebFluxConfigurer.class).run((context) -> {
					assertThat(context).getBeans(ValidatorFactory.class).hasSize(1);
					assertThat(context).getBeans(jakarta.validation.Validator.class).hasSize(1);
					assertThat(context).getBeanNames(Validator.class).containsExactlyInAnyOrder("defaultValidator",
							"webFluxValidator");
					assertThat(context.getBean("webFluxValidator"))
							.isSameAs(context.getBean(ValidatorWebFluxConfigurer.class).validator);
					// Primary Spring validator is the auto-configured one as the WebFlux
					// one has been
					// customized via a WebFluxConfigurer
					assertThat(context.getBean(Validator.class)).isEqualTo(context.getBean("defaultValidator"));
				});
	}

	@Test
	void validatorWithCustomSpringValidatorIgnored() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.withUserConfiguration(CustomSpringValidator.class).run((context) -> {
					assertThat(context).getBeanNames(jakarta.validation.Validator.class)
							.containsExactly("defaultValidator");
					assertThat(context).getBeanNames(Validator.class).containsExactlyInAnyOrder("customValidator",
							"defaultValidator", "webFluxValidator");
					Validator validator = context.getBean("webFluxValidator", Validator.class);
					assertThat(validator).isInstanceOf(ValidatorAdapter.class);
					Object defaultValidator = context.getBean("defaultValidator");
					assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
					// Primary Spring validator is the one used by WebFlux behind the
					// scenes
					assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
				});
	}

	@Test
	void validatorWithCustomJsr303ValidatorExposedAsSpringValidator() {
		this.contextRunner.withUserConfiguration(CustomJsr303Validator.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).getBeanNames(jakarta.validation.Validator.class).containsExactly("customValidator");
			assertThat(context).getBeanNames(Validator.class).containsExactly("webFluxValidator");
			Validator validator = context.getBean(Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			Validator target = ((ValidatorAdapter) validator).getTarget();
			assertThat(target).hasFieldOrPropertyWithValue("targetValidator", context.getBean("customValidator"));
		});
	}

	@Test
	void hiddenHttpMethodFilterIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HiddenHttpMethodFilter.class));
	}

	@Test
	void hiddenHttpMethodFilterCanBeOverridden() {
		this.contextRunner.withPropertyValues("spring.webflux.hiddenmethod.filter.enabled=true")
				.withUserConfiguration(CustomHiddenHttpMethodFilter.class).run((context) -> {
					assertThat(context).doesNotHaveBean(OrderedHiddenHttpMethodFilter.class);
					assertThat(context).hasSingleBean(HiddenHttpMethodFilter.class);
				});
	}

	@Test
	void hiddenHttpMethodFilterCanBeEnabled() {
		this.contextRunner.withPropertyValues("spring.webflux.hiddenmethod.filter.enabled=true")
				.run((context) -> assertThat(context).hasSingleBean(OrderedHiddenHttpMethodFilter.class));
	}

	@Test
	void customRequestMappingHandlerMapping() {
		this.contextRunner.withUserConfiguration(CustomRequestMappingHandlerMapping.class).run((context) -> {
			assertThat(context).getBean(RequestMappingHandlerMapping.class)
					.isInstanceOf(MyRequestMappingHandlerMapping.class);
			assertThat(context.getBean(CustomRequestMappingHandlerMapping.class).handlerMappings).isEqualTo(1);
		});
	}

	@Test
	void customRequestMappingHandlerAdapter() {
		this.contextRunner.withUserConfiguration(CustomRequestMappingHandlerAdapter.class).run((context) -> {
			assertThat(context).getBean(RequestMappingHandlerAdapter.class)
					.isInstanceOf(MyRequestMappingHandlerAdapter.class);
			assertThat(context.getBean(CustomRequestMappingHandlerAdapter.class).handlerAdapters).isEqualTo(1);
		});
	}

	@Test
	void multipleWebFluxRegistrations() {
		this.contextRunner.withUserConfiguration(MultipleWebFluxRegistrations.class).run((context) -> {
			assertThat(context.getBean(RequestMappingHandlerMapping.class))
					.isNotInstanceOf(MyRequestMappingHandlerMapping.class);
			assertThat(context.getBean(RequestMappingHandlerAdapter.class))
					.isNotInstanceOf(MyRequestMappingHandlerAdapter.class);
		});
	}

	@Test
	void cachePeriod() {
		Assertions.setExtractBareNamePropertyMethods(false);
		this.contextRunner.withPropertyValues("spring.web.resources.cache.period:5").run((context) -> {
			Map<PathPattern, Object> handlerMap = getHandlerMap(context);
			assertThat(handlerMap).hasSize(2);
			for (Object handler : handlerMap.values()) {
				if (handler instanceof ResourceWebHandler resourceWebHandler) {
					assertThat(resourceWebHandler.getCacheControl()).usingRecursiveComparison()
							.isEqualTo(CacheControl.maxAge(5, TimeUnit.SECONDS));
				}
			}
		});
		Assertions.setExtractBareNamePropertyMethods(true);
	}

	@Test
	void cacheControl() {
		Assertions.setExtractBareNamePropertyMethods(false);
		this.contextRunner.withPropertyValues("spring.web.resources.cache.cachecontrol.max-age:5",
				"spring.web.resources.cache.cachecontrol.proxy-revalidate:true").run((context) -> {
					Map<PathPattern, Object> handlerMap = getHandlerMap(context);
					assertThat(handlerMap).hasSize(2);
					for (Object handler : handlerMap.values()) {
						if (handler instanceof ResourceWebHandler resourceWebHandler) {
							assertThat(resourceWebHandler.getCacheControl()).usingRecursiveComparison()
									.isEqualTo(CacheControl.maxAge(5, TimeUnit.SECONDS).proxyRevalidate());
						}
					}
				});
		Assertions.setExtractBareNamePropertyMethods(true);
	}

	@Test
	void useLastModified() {
		this.contextRunner.withPropertyValues("spring.web.resources.cache.use-last-modified=false").run((context) -> {
			Map<PathPattern, Object> handlerMap = getHandlerMap(context);
			assertThat(handlerMap).hasSize(2);
			for (Object handler : handlerMap.values()) {
				if (handler instanceof ResourceWebHandler resourceWebHandler) {
					assertThat(resourceWebHandler.isUseLastModified()).isFalse();
				}
			}
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
	void welcomePageHandlerMapping() {
		this.contextRunner.withPropertyValues("spring.web.resources.static-locations=classpath:/welcome-page/")
				.run((context) -> {
					assertThat(context).getBeans(RouterFunctionMapping.class).hasSize(2);
					assertThat(context.getBean("welcomePageRouterFunctionMapping", HandlerMapping.class)).isNotNull()
							.extracting("order").isEqualTo(1);
				});
	}

	@Test
	void defaultLocaleContextResolver() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(LocaleContextResolver.class);
			LocaleContextResolver resolver = context.getBean(LocaleContextResolver.class);
			assertThat(((AcceptHeaderLocaleContextResolver) resolver).getDefaultLocale()).isNull();
		});
	}

	@Test
	void whenFixedLocalContextResolverIsUsedThenAcceptLanguagesHeaderIsIgnored() {
		this.contextRunner.withPropertyValues("spring.web.locale:en_UK", "spring.web.locale-resolver=fixed")
				.run((context) -> {
					MockServerHttpRequest request = MockServerHttpRequest.get("/")
							.acceptLanguageAsLocales(StringUtils.parseLocaleString("nl_NL")).build();
					MockServerWebExchange exchange = MockServerWebExchange.from(request);
					LocaleContextResolver localeContextResolver = context.getBean(LocaleContextResolver.class);
					assertThat(localeContextResolver).isInstanceOf(FixedLocaleContextResolver.class);
					LocaleContext localeContext = localeContextResolver.resolveLocaleContext(exchange);
					assertThat(localeContext.getLocale()).isEqualTo(StringUtils.parseLocaleString("en_UK"));
				});
	}

	@Test
	void whenAcceptHeaderLocaleContextResolverIsUsedThenAcceptLanguagesHeaderIsHonoured() {
		this.contextRunner.withPropertyValues("spring.web.locale:en_UK").run((context) -> {
			MockServerHttpRequest request = MockServerHttpRequest.get("/")
					.acceptLanguageAsLocales(StringUtils.parseLocaleString("nl_NL")).build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			LocaleContextResolver localeContextResolver = context.getBean(LocaleContextResolver.class);
			assertThat(localeContextResolver).isInstanceOf(AcceptHeaderLocaleContextResolver.class);
			LocaleContext localeContext = localeContextResolver.resolveLocaleContext(exchange);
			assertThat(localeContext.getLocale()).isEqualTo(StringUtils.parseLocaleString("nl_NL"));
		});
	}

	@Test
	void whenAcceptHeaderLocaleContextResolverIsUsedAndHeaderIsAbsentThenConfiguredLocaleIsUsed() {
		this.contextRunner.withPropertyValues("spring.web.locale:en_UK").run((context) -> {
			MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			LocaleContextResolver localeContextResolver = context.getBean(LocaleContextResolver.class);
			assertThat(localeContextResolver).isInstanceOf(AcceptHeaderLocaleContextResolver.class);
			LocaleContext localeContext = localeContextResolver.resolveLocaleContext(exchange);
			assertThat(localeContext.getLocale()).isEqualTo(StringUtils.parseLocaleString("en_UK"));
		});
	}

	@Test
	void customLocaleContextResolverWithMatchingNameReplacedAutoConfiguredLocaleContextResolver() {
		this.contextRunner
				.withBean("localeContextResolver", CustomLocaleContextResolver.class, CustomLocaleContextResolver::new)
				.run((context) -> {
					assertThat(context).hasSingleBean(LocaleContextResolver.class);
					assertThat(context.getBean("localeContextResolver"))
							.isInstanceOf(CustomLocaleContextResolver.class);
				});
	}

	@Test
	void customLocaleContextResolverWithDifferentNameDoesNotReplaceAutoConfiguredLocaleContextResolver() {
		this.contextRunner.withBean("customLocaleContextResolver", CustomLocaleContextResolver.class,
				CustomLocaleContextResolver::new).run((context) -> {
					assertThat(context.getBean("customLocaleContextResolver"))
							.isInstanceOf(CustomLocaleContextResolver.class);
					assertThat(context.getBean("localeContextResolver"))
							.isInstanceOf(AcceptHeaderLocaleContextResolver.class);
				});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void userConfigurersCanBeOrderedBeforeOrAfterTheAutoConfiguredConfigurer() {
		this.contextRunner.withBean(HighPrecedenceConfigurer.class, HighPrecedenceConfigurer::new)
				.withBean(LowPrecedenceConfigurer.class, LowPrecedenceConfigurer::new)
				.run((context) -> assertThat(context.getBean(DelegatingWebFluxConfiguration.class))
						.extracting("configurers.delegates").asList()
						.extracting((configurer) -> (Class) configurer.getClass()).containsExactly(
								HighPrecedenceConfigurer.class, WebFluxConfig.class, LowPrecedenceConfigurer.class));
	}

	@Test
	void customWebSessionIdResolverShouldBeApplied() {
		this.contextRunner.withUserConfiguration(CustomWebSessionIdResolver.class).run(assertExchangeWithSession(
				(exchange) -> assertThat(exchange.getResponse().getCookies().get("TEST")).isNotEmpty()));
	}

	@Test
	void customSessionTimeoutConfigurationShouldBeApplied() {
		this.contextRunner.withPropertyValues("server.reactive.session.timeout:123")
				.run((assertSessionTimeoutWithWebSession((webSession) -> {
					webSession.start();
					assertThat(webSession.getMaxIdleTime()).hasSeconds(123);
				})));
	}

	@Test
	void customSessionCookieConfigurationShouldBeApplied() {
		this.contextRunner.withPropertyValues("server.reactive.session.cookie.name:JSESSIONID",
				"server.reactive.session.cookie.domain:.example.com", "server.reactive.session.cookie.path:/example",
				"server.reactive.session.cookie.max-age:60", "server.reactive.session.cookie.http-only:false",
				"server.reactive.session.cookie.secure:false", "server.reactive.session.cookie.same-site:strict")
				.run(assertExchangeWithSession((exchange) -> {
					List<ResponseCookie> cookies = exchange.getResponse().getCookies().get("JSESSIONID");
					assertThat(cookies).isNotEmpty();
					assertThat(cookies).allMatch((cookie) -> cookie.getDomain().equals(".example.com"));
					assertThat(cookies).allMatch((cookie) -> cookie.getPath().equals("/example"));
					assertThat(cookies).allMatch((cookie) -> cookie.getMaxAge().equals(Duration.ofSeconds(60)));
					assertThat(cookies).allMatch((cookie) -> !cookie.isHttpOnly());
					assertThat(cookies).allMatch((cookie) -> !cookie.isSecure());
					assertThat(cookies).allMatch((cookie) -> cookie.getSameSite().equals("Strict"));
				}));
	}

	@ParameterizedTest
	@ValueSource(classes = { ServerProperties.class, WebFluxProperties.class })
	void propertiesAreNotEnabledInNonWebApplication(Class<?> propertiesClass) {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class,
						WebSessionIdResolverAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(propertiesClass));
	}

	private ContextConsumer<ReactiveWebApplicationContext> assertExchangeWithSession(
			Consumer<MockServerWebExchange> exchange) {
		return (context) -> {
			MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
			MockServerWebExchange webExchange = MockServerWebExchange.from(request);
			WebSessionManager webSessionManager = context.getBean(WebSessionManager.class);
			WebSession webSession = webSessionManager.getSession(webExchange).block();
			webSession.start();
			webExchange.getResponse().setComplete().block();
			exchange.accept(webExchange);
		};
	}

	private ContextConsumer<ReactiveWebApplicationContext> assertSessionTimeoutWithWebSession(
			Consumer<WebSession> session) {
		return (context) -> {
			MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
			MockServerWebExchange webExchange = MockServerWebExchange.from(request);
			WebSessionManager webSessionManager = context.getBean(WebSessionManager.class);
			WebSession webSession = webSessionManager.getSession(webExchange).block();
			session.accept(webSession);
		};
	}

	private Map<PathPattern, Object> getHandlerMap(ApplicationContext context) {
		HandlerMapping mapping = context.getBean("resourceHandlerMapping", HandlerMapping.class);
		if (mapping instanceof SimpleUrlHandlerMapping simpleMapping) {
			return simpleMapping.getHandlerMap();
		}
		return Collections.emptyMap();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebSessionIdResolver {

		@Bean
		WebSessionIdResolver webSessionIdResolver() {
			CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
			resolver.setCookieName("TEST");
			return resolver;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomArgumentResolvers {

		@Bean
		HandlerMethodArgumentResolver firstResolver() {
			return mock(HandlerMethodArgumentResolver.class);
		}

		@Bean
		HandlerMethodArgumentResolver secondResolver() {
			return mock(HandlerMethodArgumentResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCodecCustomizers {

		@Bean
		CodecCustomizer firstCodecCustomizer() {
			return mock(CodecCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ViewResolvers {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		ViewResolver aViewResolver() {
			return mock(ViewResolver.class);
		}

		@Bean
		ViewResolver anotherViewResolver() {
			return mock(ViewResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return mockReactiveWebServerFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHttpHandler {

		@Bean
		HttpHandler httpHandler() {
			return (serverHttpRequest, serverHttpResponse) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ValidatorWebFluxConfigurer implements WebFluxConfigurer {

		private final Validator validator = mock(Validator.class);

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ValidatorJsr303WebFluxConfigurer implements WebFluxConfigurer {

		private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJsr303Validator {

		@Bean
		jakarta.validation.Validator customValidator() {
			return mock(jakarta.validation.Validator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSpringValidator {

		@Bean
		Validator customValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHiddenHttpMethodFilter {

		@Bean
		HiddenHttpMethodFilter customHiddenHttpMethodFilter() {
			return mock(HiddenHttpMethodFilter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRequestMappingHandlerAdapter {

		private int handlerAdapters = 0;

		@Bean
		WebFluxRegistrations webFluxRegistrationsHandlerAdapter() {
			return new WebFluxRegistrations() {

				@Override
				public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
					CustomRequestMappingHandlerAdapter.this.handlerAdapters++;
					return new WebFluxAutoConfigurationTests.MyRequestMappingHandlerAdapter();
				}

			};
		}

	}

	static class MyRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ WebFluxAutoConfigurationTests.CustomRequestMappingHandlerMapping.class,
			WebFluxAutoConfigurationTests.CustomRequestMappingHandlerAdapter.class })
	static class MultipleWebFluxRegistrations {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRequestMappingHandlerMapping {

		private int handlerMappings = 0;

		@Bean
		WebFluxRegistrations webFluxRegistrationsHandlerMapping() {
			return new WebFluxRegistrations() {

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

	static class CustomLocaleContextResolver implements LocaleContextResolver {

		@Override
		public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
			return () -> Locale.ENGLISH;
		}

		@Override
		public void setLocaleContext(ServerWebExchange exchange, LocaleContext localeContext) {
		}

	}

	@Order(-100)
	static class HighPrecedenceConfigurer implements WebFluxConfigurer {

	}

	@Order(100)
	static class LowPrecedenceConfigurer implements WebFluxConfigurer {

	}

}
