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

package org.springframework.boot.webflux.autoconfigure;

import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.WebResourcesRuntimeHints;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.validation.autoconfigure.ValidatorAdapter;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties.Apiversion;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties.Apiversion.Use;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties.Format;
import org.springframework.boot.webflux.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.reactive.accept.ApiVersionDeprecationHandler;
import org.springframework.web.reactive.accept.ApiVersionResolver;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.BlockingExecutionConfigurer;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.InMemoryWebSessionStore;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebFlux WebFlux}.
 *
 * @author Brian Clozel
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Artsiom Yudovin
 * @author Chris Bono
 * @author Weix Sun
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { ReactiveMultipartAutoConfiguration.class, WebSessionIdResolverAutoConfiguration.class,
				CodecsAutoConfiguration.class },
		afterName = "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration")
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@ConditionalOnMissingBean({ WebFluxConfigurationSupport.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@ImportRuntimeHints(WebResourcesRuntimeHints.class)
public final class WebFluxAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnBooleanProperty("spring.webflux.hiddenmethod.filter.enabled")
	OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	@Configuration(proxyBeanMethods = false)
	static class WelcomePageConfiguration {

		@Bean
		RouterFunctionMapping welcomePageRouterFunctionMapping(ApplicationContext applicationContext,
				WebFluxProperties webFluxProperties, WebProperties webProperties) {
			String[] staticLocations = webProperties.getResources().getStaticLocations();
			WelcomePageRouterFunctionFactory factory = new WelcomePageRouterFunctionFactory(
					new TemplateAvailabilityProviders(applicationContext), applicationContext, staticLocations,
					webFluxProperties.getStaticPathPattern());
			RouterFunction<ServerResponse> routerFunction = factory.createRouterFunction();
			if (routerFunction != null) {
				RouterFunctionMapping routerFunctionMapping = new RouterFunctionMapping(routerFunction);
				routerFunctionMapping.setOrder(1);
				return routerFunctionMapping;
			}
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ WebProperties.class, WebFluxProperties.class })
	@Import({ EnableWebFluxConfiguration.class })
	@Order(0)
	static class WebFluxConfig implements WebFluxConfigurer {

		private static final Log logger = LogFactory.getLog(WebFluxConfig.class);

		private final Environment environment;

		private final Resources resourceProperties;

		private final WebFluxProperties webFluxProperties;

		private final ListableBeanFactory beanFactory;

		private final ObjectProvider<HandlerMethodArgumentResolver> argumentResolvers;

		private final ObjectProvider<CodecCustomizer> codecCustomizers;

		private final ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizers;

		private final ObjectProvider<ViewResolver> viewResolvers;

		private final ObjectProvider<ApiVersionResolver> apiVersionResolvers;

		private final ObjectProvider<ApiVersionParser<?>> apiVersionParser;

		private final ObjectProvider<ApiVersionDeprecationHandler> apiVersionDeprecationHandler;

		private final ObjectProvider<ApiVersionCustomizer> apiVersionCustomizers;

		WebFluxConfig(Environment environment, WebProperties webProperties, WebFluxProperties webFluxProperties,
				ListableBeanFactory beanFactory, ObjectProvider<HandlerMethodArgumentResolver> resolvers,
				ObjectProvider<CodecCustomizer> codecCustomizers,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizers,
				ObjectProvider<ViewResolver> viewResolvers, ObjectProvider<ApiVersionResolver> apiVersionResolvers,
				ObjectProvider<ApiVersionParser<?>> apiVersionParser,
				ObjectProvider<ApiVersionDeprecationHandler> apiVersionDeprecationHandler,
				ObjectProvider<ApiVersionCustomizer> apiVersionCustomizers) {
			this.environment = environment;
			this.resourceProperties = webProperties.getResources();
			this.webFluxProperties = webFluxProperties;
			this.beanFactory = beanFactory;
			this.argumentResolvers = resolvers;
			this.codecCustomizers = codecCustomizers;
			this.resourceHandlerRegistrationCustomizers = resourceHandlerRegistrationCustomizers;
			this.viewResolvers = viewResolvers;
			this.apiVersionResolvers = apiVersionResolvers;
			this.apiVersionParser = apiVersionParser;
			this.apiVersionDeprecationHandler = apiVersionDeprecationHandler;
			this.apiVersionCustomizers = apiVersionCustomizers;
		}

		@Override
		public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
			this.argumentResolvers.orderedStream().forEach(configurer::addCustomResolver);
		}

		@Override
		public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			this.codecCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configurer));
		}

		@Override
		public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
			if (Threading.VIRTUAL.isActive(this.environment) && this.beanFactory
				.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)) {
				Object taskExecutor = this.beanFactory
					.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
				if (taskExecutor instanceof AsyncTaskExecutor asyncTaskExecutor) {
					configurer.setExecutor(asyncTaskExecutor);
				}
			}
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (!this.resourceProperties.isAddMappings()) {
				logger.debug("Default resource handling disabled");
				return;
			}
			List<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizers = this.resourceHandlerRegistrationCustomizers
				.orderedStream()
				.toList();
			String webjarsPathPattern = this.webFluxProperties.getWebjarsPathPattern();
			if (!registry.hasMappingForPattern(webjarsPathPattern)) {
				ResourceHandlerRegistration registration = registry.addResourceHandler(webjarsPathPattern)
					.addResourceLocations("classpath:/META-INF/resources/webjars/");
				configureResourceCaching(registration);
				resourceHandlerRegistrationCustomizers.forEach((customizer) -> customizer.customize(registration));
			}
			String staticPathPattern = this.webFluxProperties.getStaticPathPattern();
			if (!registry.hasMappingForPattern(staticPathPattern)) {
				ResourceHandlerRegistration registration = registry.addResourceHandler(staticPathPattern)
					.addResourceLocations(this.resourceProperties.getStaticLocations());
				configureResourceCaching(registration);
				resourceHandlerRegistrationCustomizers.forEach((customizer) -> customizer.customize(registration));
			}
		}

		private void configureResourceCaching(ResourceHandlerRegistration registration) {
			Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
			WebProperties.Resources.Cache.Cachecontrol cacheControl = this.resourceProperties.getCache()
				.getCachecontrol();
			if (cachePeriod != null && cacheControl.getMaxAge() == null) {
				cacheControl.setMaxAge(cachePeriod);
			}
			registration.setCacheControl(cacheControl.toHttpCacheControl());
			registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			this.viewResolvers.orderedStream().forEach(registry::viewResolver);
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			ApplicationConversionService.addBeans(registry, this.beanFactory);
		}

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			Apiversion properties = this.webFluxProperties.getApiversion();
			map.from(properties::getRequired).to(configurer::setVersionRequired);
			map.from(properties::getDefaultVersion).to(configurer::setDefaultVersion);
			map.from(properties::getSupported).to((supported) -> supported.forEach(configurer::addSupportedVersions));
			map.from(properties::getDetectSupported).to(configurer::detectSupportedVersions);
			configureApiVersioningUse(configurer, properties.getUse());
			this.apiVersionResolvers.orderedStream().forEach(configurer::useVersionResolver);
			this.apiVersionParser.ifAvailable(configurer::setVersionParser);
			this.apiVersionDeprecationHandler.ifAvailable(configurer::setDeprecationHandler);
			this.apiVersionCustomizers.orderedStream().forEach(customizer -> customizer.customize(configurer));
		}

		private void configureApiVersioningUse(ApiVersionConfigurer configurer, Use use) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(use::getHeader).whenHasText().to(configurer::useRequestHeader);
			map.from(use::getQueryParameter).whenHasText().to(configurer::useQueryParam);
			map.from(use::getPathSegment).to(configurer::usePathSegment);
			use.getMediaTypeParameter()
				.forEach((mediaType, parameterName) -> configurer.useMediaTypeParameter(mediaType, parameterName));
		}

	}

	/**
	 * Configuration equivalent to {@code @EnableWebFlux}.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ WebProperties.class, ServerProperties.class })
	static class EnableWebFluxConfiguration extends DelegatingWebFluxConfiguration {

		private final WebFluxProperties webFluxProperties;

		private final WebProperties webProperties;

		private final ServerProperties serverProperties;

		private final WebFluxRegistrations webFluxRegistrations;

		EnableWebFluxConfiguration(WebFluxProperties webFluxProperties, WebProperties webProperties,
				ServerProperties serverProperties, ObjectProvider<WebFluxRegistrations> webFluxRegistrations) {
			this.webFluxProperties = webFluxProperties;
			this.webProperties = webProperties;
			this.serverProperties = serverProperties;
			this.webFluxRegistrations = webFluxRegistrations.getIfUnique();
		}

		@Bean
		@Override
		public FormattingConversionService webFluxConversionService() {
			Format format = this.webFluxProperties.getFormat();
			WebConversionService conversionService = new WebConversionService(
					new DateTimeFormatters().dateFormat(format.getDate())
						.timeFormat(format.getTime())
						.dateTimeFormat(format.getDateTime()));
			addFormatters(conversionService);
			return conversionService;
		}

		@Bean
		@Override
		public Validator webFluxValidator() {
			if (!ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())
					|| !ClassUtils.isPresent("org.springframework.boot.validation.autoconfigure.ValidatorAdapter",
							getClass().getClassLoader())) {
				return super.webFluxValidator();
			}
			return ValidatorAdapter.get(getApplicationContext(), getValidator());
		}

		@Override
		protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
			if (this.webFluxRegistrations != null) {
				RequestMappingHandlerAdapter adapter = this.webFluxRegistrations.getRequestMappingHandlerAdapter();
				if (adapter != null) {
					return adapter;
				}
			}
			return super.createRequestMappingHandlerAdapter();
		}

		@Override
		protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
			if (this.webFluxRegistrations != null) {
				RequestMappingHandlerMapping mapping = this.webFluxRegistrations.getRequestMappingHandlerMapping();
				if (mapping != null) {
					return mapping;
				}
			}
			return super.createRequestMappingHandlerMapping();
		}

		@Bean
		@Override
		@ConditionalOnMissingBean(name = WebHttpHandlerBuilder.LOCALE_CONTEXT_RESOLVER_BEAN_NAME)
		public LocaleContextResolver localeContextResolver() {
			if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
				return new FixedLocaleContextResolver(this.webProperties.getLocale());
			}
			AcceptHeaderLocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();
			localeContextResolver.setDefaultLocale(this.webProperties.getLocale());
			return localeContextResolver;
		}

		@Bean
		@ConditionalOnMissingBean(name = WebHttpHandlerBuilder.WEB_SESSION_MANAGER_BEAN_NAME)
		WebSessionManager webSessionManager(ObjectProvider<WebSessionIdResolver> webSessionIdResolver) {
			DefaultWebSessionManager webSessionManager = new DefaultWebSessionManager();
			Duration timeout = this.serverProperties.getReactive().getSession().getTimeout();
			int maxSessions = this.serverProperties.getReactive().getSession().getMaxSessions();
			MaxIdleTimeInMemoryWebSessionStore sessionStore = new MaxIdleTimeInMemoryWebSessionStore(timeout);
			sessionStore.setMaxSessions(maxSessions);
			webSessionManager.setSessionStore(sessionStore);
			webSessionIdResolver.ifAvailable(webSessionManager::setSessionIdResolver);
			return webSessionManager;
		}

		@Override
		@ConditionalOnMissingBean(name = "webFluxApiVersionStrategy")
		public @Nullable ApiVersionStrategy webFluxApiVersionStrategy() {
			return super.webFluxApiVersionStrategy();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnEnabledResourceChain
	static class ResourceChainCustomizerConfiguration {

		@Bean
		ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
				WebProperties webProperties) {
			return new ResourceChainResourceHandlerRegistrationCustomizer(webProperties.getResources());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty("spring.webflux.problemdetails.enabled")
	static class ProblemDetailsErrorHandlingConfiguration {

		@Bean
		@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
		@Order(0)
		ProblemDetailsExceptionHandler problemDetailsExceptionHandler() {
			return new ProblemDetailsExceptionHandler();
		}

	}

	static final class MaxIdleTimeInMemoryWebSessionStore extends InMemoryWebSessionStore {

		private final Duration timeout;

		private MaxIdleTimeInMemoryWebSessionStore(Duration timeout) {
			this.timeout = timeout;
		}

		@Override
		public Mono<WebSession> createWebSession() {
			return super.createWebSession().doOnSuccess(this::setMaxIdleTime);
		}

		private void setMaxIdleTime(WebSession session) {
			session.setMaxIdleTime(this.timeout);
		}

	}

}
