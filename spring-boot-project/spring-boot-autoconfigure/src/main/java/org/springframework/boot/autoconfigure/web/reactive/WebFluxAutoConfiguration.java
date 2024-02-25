/*
 * Copyright 2012-2024 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.WebResourcesRuntimeHints;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties.Format;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
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
 * @since 2.0.0
 */
@AutoConfiguration(after = { ReactiveWebServerFactoryAutoConfiguration.class, CodecsAutoConfiguration.class,
		ReactiveMultipartAutoConfiguration.class, ValidationAutoConfiguration.class,
		WebSessionIdResolverAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@ConditionalOnMissingBean({ WebFluxConfigurationSupport.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@ImportRuntimeHints(WebResourcesRuntimeHints.class)
public class WebFluxAutoConfiguration {

	/**
     * Creates and configures an instance of {@link OrderedHiddenHttpMethodFilter} if a bean of type {@link HiddenHttpMethodFilter} is not already present in the application context and if the property "spring.webflux.hiddenmethod.filter.enabled" is set to true.
     * 
     * This filter is responsible for enabling support for hidden HTTP method parameters in Spring WebFlux applications.
     * 
     * @return the configured instance of {@link OrderedHiddenHttpMethodFilter}
     */
    @Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnProperty(prefix = "spring.webflux.hiddenmethod.filter", name = "enabled")
	public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	/**
     * WelcomePageConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	public static class WelcomePageConfiguration {

		/**
         * Creates a RouterFunctionMapping for the welcome page.
         * 
         * @param applicationContext The ApplicationContext for the application.
         * @param webFluxProperties The WebFluxProperties for the application.
         * @param webProperties The WebProperties for the application.
         * @return The RouterFunctionMapping for the welcome page, or null if no router function is created.
         */
        @Bean
		public RouterFunctionMapping welcomePageRouterFunctionMapping(ApplicationContext applicationContext,
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

	/**
     * WebFluxConfig class.
     */
    @Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ WebProperties.class, WebFluxProperties.class })
	@Import({ EnableWebFluxConfiguration.class })
	@Order(0)
	public static class WebFluxConfig implements WebFluxConfigurer {

		private static final Log logger = LogFactory.getLog(WebFluxConfig.class);

		private final Resources resourceProperties;

		private final WebFluxProperties webFluxProperties;

		private final ListableBeanFactory beanFactory;

		private final ObjectProvider<HandlerMethodArgumentResolver> argumentResolvers;

		private final ObjectProvider<CodecCustomizer> codecCustomizers;

		private final ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;

		private final ObjectProvider<ViewResolver> viewResolvers;

		/**
         * Constructs a new WebFluxConfig with the specified parameters.
         * 
         * @param webProperties the WebProperties object containing web-related properties
         * @param webFluxProperties the WebFluxProperties object containing WebFlux-related properties
         * @param beanFactory the ListableBeanFactory used for bean resolution
         * @param resolvers the ObjectProvider of HandlerMethodArgumentResolver used for resolving method arguments
         * @param codecCustomizers the ObjectProvider of CodecCustomizer used for customizing codecs
         * @param resourceHandlerRegistrationCustomizer the ObjectProvider of ResourceHandlerRegistrationCustomizer used for customizing resource handler registration
         * @param viewResolvers the ObjectProvider of ViewResolver used for resolving views
         */
        public WebFluxConfig(WebProperties webProperties, WebFluxProperties webFluxProperties,
				ListableBeanFactory beanFactory, ObjectProvider<HandlerMethodArgumentResolver> resolvers,
				ObjectProvider<CodecCustomizer> codecCustomizers,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizer,
				ObjectProvider<ViewResolver> viewResolvers) {
			this.resourceProperties = webProperties.getResources();
			this.webFluxProperties = webFluxProperties;
			this.beanFactory = beanFactory;
			this.argumentResolvers = resolvers;
			this.codecCustomizers = codecCustomizers;
			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizer.getIfAvailable();
			this.viewResolvers = viewResolvers;
		}

		/**
         * Configures the argument resolvers for the WebFlux application.
         * 
         * @param configurer the argument resolver configurer
         */
        @Override
		public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
			this.argumentResolvers.orderedStream().forEach(configurer::addCustomResolver);
		}

		/**
         * Configures the HTTP message codecs for the server.
         * 
         * @param configurer the server codec configurer
         */
        @Override
		public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			this.codecCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configurer));
		}

		/**
         * Configures the blocking execution for the application.
         * 
         * @param configurer the BlockingExecutionConfigurer to be used for configuration
         */
        @Override
		public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
			if (this.beanFactory.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)) {
				Object taskExecutor = this.beanFactory
					.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
				if (taskExecutor instanceof AsyncTaskExecutor asyncTaskExecutor) {
					configurer.setExecutor(asyncTaskExecutor);
				}
			}
		}

		/**
         * Adds resource handlers for serving static resources.
         * 
         * @param registry the resource handler registry
         */
        @Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (!this.resourceProperties.isAddMappings()) {
				logger.debug("Default resource handling disabled");
				return;
			}
			String webjarsPathPattern = this.webFluxProperties.getWebjarsPathPattern();
			if (!registry.hasMappingForPattern(webjarsPathPattern)) {
				ResourceHandlerRegistration registration = registry.addResourceHandler(webjarsPathPattern)
					.addResourceLocations("classpath:/META-INF/resources/webjars/");
				configureResourceCaching(registration);
				customizeResourceHandlerRegistration(registration);
			}
			String staticPathPattern = this.webFluxProperties.getStaticPathPattern();
			if (!registry.hasMappingForPattern(staticPathPattern)) {
				ResourceHandlerRegistration registration = registry.addResourceHandler(staticPathPattern)
					.addResourceLocations(this.resourceProperties.getStaticLocations());
				configureResourceCaching(registration);
				customizeResourceHandlerRegistration(registration);
			}
		}

		/**
         * Configures resource caching for the given {@link ResourceHandlerRegistration}.
         * 
         * @param registration the {@link ResourceHandlerRegistration} to configure
         */
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

		/**
         * Configures the view resolvers for the WebFlux application.
         * 
         * @param registry the view resolver registry
         */
        @Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			this.viewResolvers.orderedStream().forEach(registry::viewResolver);
		}

		/**
         * Registers formatters in the given {@link FormatterRegistry}.
         * 
         * @param registry the {@link FormatterRegistry} to register formatters with
         */
        @Override
		public void addFormatters(FormatterRegistry registry) {
			ApplicationConversionService.addBeans(registry, this.beanFactory);
		}

		/**
         * Customizes the registration of a resource handler.
         * 
         * @param registration the resource handler registration to be customized
         */
        private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
			if (this.resourceHandlerRegistrationCustomizer != null) {
				this.resourceHandlerRegistrationCustomizer.customize(registration);
			}
		}

	}

	/**
	 * Configuration equivalent to {@code @EnableWebFlux}.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ WebProperties.class, ServerProperties.class })
	public static class EnableWebFluxConfiguration extends DelegatingWebFluxConfiguration {

		private final WebFluxProperties webFluxProperties;

		private final WebProperties webProperties;

		private final ServerProperties serverProperties;

		private final WebFluxRegistrations webFluxRegistrations;

		/**
         * Constructor for EnableWebFluxConfiguration class.
         * 
         * @param webFluxProperties      the WebFluxProperties object containing the properties for WebFlux configuration
         * @param webProperties          the WebProperties object containing the properties for web configuration
         * @param serverProperties       the ServerProperties object containing the properties for server configuration
         * @param webFluxRegistrations   the ObjectProvider for WebFluxRegistrations
         */
        public EnableWebFluxConfiguration(WebFluxProperties webFluxProperties, WebProperties webProperties,
				ServerProperties serverProperties, ObjectProvider<WebFluxRegistrations> webFluxRegistrations) {
			this.webFluxProperties = webFluxProperties;
			this.webProperties = webProperties;
			this.serverProperties = serverProperties;
			this.webFluxRegistrations = webFluxRegistrations.getIfUnique();
		}

		/**
         * Returns the webFluxConversionService bean.
         * 
         * This method creates and configures a FormattingConversionService bean for WebFlux.
         * The conversion service is used to convert request parameters and path variables to
         * the desired data types in the specified format.
         * 
         * The format is determined by the webFluxProperties bean, which is injected into this method.
         * The format includes date, time, and date-time formats.
         * 
         * The conversion service is created using the WebConversionService class, which is a custom
         * implementation of the FormattingConversionService interface. The date, time, and date-time
         * formats are set using the DateTimeFormatters class, which provides convenient methods for
         * creating date and time formatters based on the specified format.
         * 
         * Additional formatters can be added to the conversion service using the addFormatters method.
         * 
         * @return the webFluxConversionService bean
         */
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

		/**
         * Returns the webFluxValidator.
         * If the Jakarta Validation API is present in the classpath, it returns a ValidatorAdapter
         * that adapts the Jakarta Validator to the Spring Validator interface.
         * Otherwise, it returns the default webFluxValidator provided by the superclass.
         *
         * @return the webFluxValidator
         */
        @Bean
		@Override
		public Validator webFluxValidator() {
			if (!ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
				return super.webFluxValidator();
			}
			return ValidatorAdapter.get(getApplicationContext(), getValidator());
		}

		/**
         * Create a RequestMappingHandlerAdapter for handling request mappings.
         * If a custom RequestMappingHandlerAdapter is provided through webFluxRegistrations,
         * it will be used. Otherwise, the default RequestMappingHandlerAdapter will be used.
         * 
         * @return the created RequestMappingHandlerAdapter
         */
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

		/**
         * Creates a RequestMappingHandlerMapping for handling request mappings in a WebFlux application.
         * If webFluxRegistrations is not null, it retrieves the RequestMappingHandlerMapping from it.
         * If the mapping is not null, it returns the mapping.
         * Otherwise, it calls the super class's createRequestMappingHandlerMapping method.
         * 
         * @return the RequestMappingHandlerMapping for handling request mappings
         */
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

		/**
         * Returns the locale context resolver for the web application.
         * If the locale resolver is set to FIXED in the web properties,
         * a FixedLocaleContextResolver is returned with the specified locale.
         * Otherwise, an AcceptHeaderLocaleContextResolver is returned with
         * the default locale set in the web properties.
         *
         * @return the locale context resolver for the web application
         */
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

		/**
         * Create a {@link WebSessionManager} bean if one is not already defined.
         * The bean is created with a {@link DefaultWebSessionManager} and configured with the session timeout and maximum sessions
         * specified in the server properties. The session store is set to a {@link MaxIdleTimeInMemoryWebSessionStore} with the
         * specified timeout and maximum sessions. If a {@link WebSessionIdResolver} is available, it is set as the session ID resolver
         * for the web session manager.
         *
         * @param webSessionIdResolver the optional {@link WebSessionIdResolver} to set as the session ID resolver
         * @return the created {@link WebSessionManager} bean
         */
        @Bean
		@ConditionalOnMissingBean(name = WebHttpHandlerBuilder.WEB_SESSION_MANAGER_BEAN_NAME)
		public WebSessionManager webSessionManager(ObjectProvider<WebSessionIdResolver> webSessionIdResolver) {
			DefaultWebSessionManager webSessionManager = new DefaultWebSessionManager();
			Duration timeout = this.serverProperties.getReactive().getSession().getTimeout();
			int maxSessions = this.serverProperties.getReactive().getSession().getMaxSessions();
			MaxIdleTimeInMemoryWebSessionStore sessionStore = new MaxIdleTimeInMemoryWebSessionStore(timeout);
			sessionStore.setMaxSessions(maxSessions);
			webSessionManager.setSessionStore(sessionStore);
			webSessionIdResolver.ifAvailable(webSessionManager::setSessionIdResolver);
			return webSessionManager;
		}

	}

	/**
     * ResourceChainCustomizerConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnEnabledResourceChain
	static class ResourceChainCustomizerConfiguration {

		/**
         * Creates a customizer for the ResourceHandlerRegistration of the ResourceChain.
         * 
         * @param webProperties the WebProperties object containing the configuration for the web resources
         * @return a ResourceChainResourceHandlerRegistrationCustomizer object
         */
        @Bean
		ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
				WebProperties webProperties) {
			return new ResourceChainResourceHandlerRegistrationCustomizer(webProperties.getResources());
		}

	}

	/**
     * ProblemDetailsErrorHandlingConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.webflux.problemdetails", name = "enabled", havingValue = "true")
	static class ProblemDetailsErrorHandlingConfiguration {

		/**
         * Creates a new instance of ProblemDetailsExceptionHandler if there is no existing bean of type ResponseEntityExceptionHandler.
         * This handler is responsible for handling exceptions and returning Problem Details as the response.
         * The order of this handler is set to 0, indicating that it should be executed first if multiple exception handlers are present.
         * 
         * @return the ProblemDetailsExceptionHandler instance
         */
        @Bean
		@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
		@Order(0)
		ProblemDetailsExceptionHandler problemDetailsExceptionHandler() {
			return new ProblemDetailsExceptionHandler();
		}

	}

	/**
     * MaxIdleTimeInMemoryWebSessionStore class.
     */
    static final class MaxIdleTimeInMemoryWebSessionStore extends InMemoryWebSessionStore {

		private final Duration timeout;

		/**
         * Constructs a new MaxIdleTimeInMemoryWebSessionStore with the specified timeout.
         *
         * @param timeout the maximum idle time for a session in memory
         */
        private MaxIdleTimeInMemoryWebSessionStore(Duration timeout) {
			this.timeout = timeout;
		}

		/**
         * Creates a new web session and sets the maximum idle time for the session.
         * 
         * @return a Mono containing the created web session
         */
        @Override
		public Mono<WebSession> createWebSession() {
			return super.createWebSession().doOnSuccess(this::setMaxIdleTime);
		}

		/**
         * Sets the maximum idle time for the given web session.
         * 
         * @param session the web session for which the maximum idle time needs to be set
         */
        private void setMaxIdleTime(WebSession session) {
			session.setMaxIdleTime(this.timeout);
		}

	}

}
