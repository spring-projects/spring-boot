/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.time.Duration;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources.Chain.Strategy;
import org.springframework.boot.autoconfigure.web.WebResourcesRuntimeHints;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties.Format;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.boot.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceChainRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebMvc Web MVC}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Sébastien Deleuze
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Kristine Jetzke
 * @author Bruce Brouwer
 * @author Artsiom Yudovin
 * @author Scott Frederick
 * @since 2.0.0
 */
@AutoConfiguration(after = { DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class,
		ValidationAutoConfiguration.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@ImportRuntimeHints(WebResourcesRuntimeHints.class)
public class WebMvcAutoConfiguration {

	/**
	 * The default Spring MVC view prefix.
	 */
	public static final String DEFAULT_PREFIX = "";

	/**
	 * The default Spring MVC view suffix.
	 */
	public static final String DEFAULT_SUFFIX = "";

	private static final String SERVLET_LOCATION = "/";

	/**
	 * Creates and configures an instance of {@link OrderedHiddenHttpMethodFilter} if a
	 * bean of type {@link HiddenHttpMethodFilter} is not already present in the
	 * application context and if the property "spring.mvc.hiddenmethod.filter.enabled" is
	 * set to true.
	 * @return the configured instance of {@link OrderedHiddenHttpMethodFilter}
	 */
	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnProperty(prefix = "spring.mvc.hiddenmethod.filter", name = "enabled")
	public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	/**
	 * Creates a new instance of {@link OrderedFormContentFilter} if no bean of type
	 * {@link FormContentFilter} is present in the application context. The filter is
	 * enabled by default unless the property "spring.mvc.formcontent.filter.enabled" is
	 * set to false.
	 * @return the created {@link OrderedFormContentFilter} instance
	 */
	@Bean
	@ConditionalOnMissingBean(FormContentFilter.class)
	@ConditionalOnProperty(prefix = "spring.mvc.formcontent.filter", name = "enabled", matchIfMissing = true)
	public OrderedFormContentFilter formContentFilter() {
		return new OrderedFormContentFilter();
	}

	// Defined as a nested config to ensure WebMvcConfigurer is not read when not
	// on the classpath
	@Configuration(proxyBeanMethods = false)
	@Import(EnableWebMvcConfiguration.class)
	@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
	@Order(0)
	public static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer, ServletContextAware {

		private static final Log logger = LogFactory.getLog(WebMvcConfigurer.class);

		private final Resources resourceProperties;

		private final WebMvcProperties mvcProperties;

		private final ListableBeanFactory beanFactory;

		private final ObjectProvider<HttpMessageConverters> messageConvertersProvider;

		private final ObjectProvider<DispatcherServletPath> dispatcherServletPath;

		private final ObjectProvider<ServletRegistrationBean<?>> servletRegistrations;

		private final ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;

		private ServletContext servletContext;

		/**
		 * Constructs a new instance of the {@code WebMvcAutoConfigurationAdapter} class.
		 * @param webProperties the {@code WebProperties} object containing web-related
		 * properties
		 * @param mvcProperties the {@code WebMvcProperties} object containing MVC-related
		 * properties
		 * @param beanFactory the {@code ListableBeanFactory} object used for accessing
		 * beans
		 * @param messageConvertersProvider the {@code ObjectProvider} for
		 * {@code HttpMessageConverters}
		 * @param resourceHandlerRegistrationCustomizerProvider the {@code ObjectProvider}
		 * for {@code ResourceHandlerRegistrationCustomizer}
		 * @param dispatcherServletPath the {@code ObjectProvider} for
		 * {@code DispatcherServletPath}
		 * @param servletRegistrations the {@code ObjectProvider} for
		 * {@code ServletRegistrationBean}
		 */
		public WebMvcAutoConfigurationAdapter(WebProperties webProperties, WebMvcProperties mvcProperties,
				ListableBeanFactory beanFactory, ObjectProvider<HttpMessageConverters> messageConvertersProvider,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
				ObjectProvider<DispatcherServletPath> dispatcherServletPath,
				ObjectProvider<ServletRegistrationBean<?>> servletRegistrations) {
			this.resourceProperties = webProperties.getResources();
			this.mvcProperties = mvcProperties;
			this.beanFactory = beanFactory;
			this.messageConvertersProvider = messageConvertersProvider;
			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizerProvider.getIfAvailable();
			this.dispatcherServletPath = dispatcherServletPath;
			this.servletRegistrations = servletRegistrations;
		}

		/**
		 * Sets the ServletContext for this instance.
		 * @param servletContext the ServletContext to be set
		 */
		@Override
		public void setServletContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		/**
		 * Configure the message converters for the application.
		 * @param converters the list of HTTP message converters
		 */
		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			this.messageConvertersProvider
				.ifAvailable((customConverters) -> converters.addAll(customConverters.getConverters()));
		}

		/**
		 * Configure asynchronous support for the application.
		 * @param configurer the AsyncSupportConfigurer to be configured
		 */
		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			if (this.beanFactory.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)) {
				Object taskExecutor = this.beanFactory
					.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
				if (taskExecutor instanceof AsyncTaskExecutor asyncTaskExecutor) {
					configurer.setTaskExecutor(asyncTaskExecutor);
				}
			}
			Duration timeout = this.mvcProperties.getAsync().getRequestTimeout();
			if (timeout != null) {
				configurer.setDefaultTimeout(timeout.toMillis());
			}
		}

		/**
		 * Configure the path matching strategy for the DispatcherServlet. If the matching
		 * strategy is set to ANT_PATH_MATCHER, set the path matcher to AntPathMatcher. If
		 * the servlet URL mapping is "/", set the URL path helper to always use the full
		 * path.
		 * @param configurer the PathMatchConfigurer to configure the path matching
		 * strategy
		 */
		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			if (this.mvcProperties.getPathmatch()
				.getMatchingStrategy() == WebMvcProperties.MatchingStrategy.ANT_PATH_MATCHER) {
				configurer.setPathMatcher(new AntPathMatcher());
				this.dispatcherServletPath.ifAvailable((dispatcherPath) -> {
					String servletUrlMapping = dispatcherPath.getServletUrlMapping();
					if (servletUrlMapping.equals("/") && singleDispatcherServlet()) {
						UrlPathHelper urlPathHelper = new UrlPathHelper();
						urlPathHelper.setAlwaysUseFullPath(true);
						configurer.setUrlPathHelper(urlPathHelper);
					}
				});
			}
		}

		/**
		 * Checks if there is only one DispatcherServlet registered.
		 * @return true if there is only one DispatcherServlet registered, false otherwise
		 */
		private boolean singleDispatcherServlet() {
			return this.servletRegistrations.stream()
				.map(ServletRegistrationBean::getServlet)
				.filter(DispatcherServlet.class::isInstance)
				.count() == 1;
		}

		/**
		 * Configure content negotiation for the application.
		 * @param configurer the ContentNegotiationConfigurer to be configured
		 */
		@Override
		public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
			WebMvcProperties.Contentnegotiation contentnegotiation = this.mvcProperties.getContentnegotiation();
			configurer.favorParameter(contentnegotiation.isFavorParameter());
			if (contentnegotiation.getParameterName() != null) {
				configurer.parameterName(contentnegotiation.getParameterName());
			}
			Map<String, MediaType> mediaTypes = this.mvcProperties.getContentnegotiation().getMediaTypes();
			mediaTypes.forEach(configurer::mediaType);
		}

		/**
		 * Creates a default InternalResourceViewResolver bean if no other bean of the
		 * same type is present. The resolver is configured with the prefix and suffix
		 * specified in the mvcProperties.
		 * @return the default InternalResourceViewResolver bean
		 */
		@Bean
		@ConditionalOnMissingBean
		public InternalResourceViewResolver defaultViewResolver() {
			InternalResourceViewResolver resolver = new InternalResourceViewResolver();
			resolver.setPrefix(this.mvcProperties.getView().getPrefix());
			resolver.setSuffix(this.mvcProperties.getView().getSuffix());
			return resolver;
		}

		/**
		 * Creates a {@link BeanNameViewResolver} bean if a {@link View} bean is present
		 * and no other {@link BeanNameViewResolver} bean is defined. The created resolver
		 * has an order of {@link Ordered#LOWEST_PRECEDENCE - 10}.
		 * @return the created {@link BeanNameViewResolver} bean
		 */
		@Bean
		@ConditionalOnBean(View.class)
		@ConditionalOnMissingBean
		public BeanNameViewResolver beanNameViewResolver() {
			BeanNameViewResolver resolver = new BeanNameViewResolver();
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
			return resolver;
		}

		/**
		 * Creates a ContentNegotiatingViewResolver bean if a ViewResolver bean is present
		 * and a ContentNegotiatingViewResolver bean is not already defined.
		 * @param beanFactory the BeanFactory used to retrieve the
		 * ContentNegotiationManager bean
		 * @return the ContentNegotiatingViewResolver bean
		 */
		@Bean
		@ConditionalOnBean(ViewResolver.class)
		@ConditionalOnMissingBean(name = "viewResolver", value = ContentNegotiatingViewResolver.class)
		public ContentNegotiatingViewResolver viewResolver(BeanFactory beanFactory) {
			ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
			resolver.setContentNegotiationManager(beanFactory.getBean(ContentNegotiationManager.class));
			// ContentNegotiatingViewResolver uses all the other view resolvers to locate
			// a view so it should have a high precedence
			resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return resolver;
		}

		/**
		 * Returns the message codes resolver for this configuration.
		 * @return the message codes resolver, or null if not set
		 */
		@Override
		public MessageCodesResolver getMessageCodesResolver() {
			if (this.mvcProperties.getMessageCodesResolverFormat() != null) {
				DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();
				resolver.setMessageCodeFormatter(this.mvcProperties.getMessageCodesResolverFormat());
				return resolver;
			}
			return null;
		}

		/**
		 * Add formatters to the given registry.
		 * @param registry the formatter registry to add formatters to
		 */
		@Override
		public void addFormatters(FormatterRegistry registry) {
			ApplicationConversionService.addBeans(registry, this.beanFactory);
		}

		/**
		 * Adds resource handlers for serving static resources.
		 * @param registry the resource handler registry
		 */
		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (!this.resourceProperties.isAddMappings()) {
				logger.debug("Default resource handling disabled");
				return;
			}
			addResourceHandler(registry, this.mvcProperties.getWebjarsPathPattern(),
					"classpath:/META-INF/resources/webjars/");
			addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
				registration.addResourceLocations(this.resourceProperties.getStaticLocations());
				if (this.servletContext != null) {
					ServletContextResource resource = new ServletContextResource(this.servletContext, SERVLET_LOCATION);
					registration.addResourceLocations(resource);
				}
			});
		}

		/**
		 * Adds a resource handler to the specified registry with the given pattern and
		 * locations.
		 * @param registry the resource handler registry
		 * @param pattern the URL pattern for the resource handler
		 * @param locations the resource locations to be served by the handler
		 */
		private void addResourceHandler(ResourceHandlerRegistry registry, String pattern, String... locations) {
			addResourceHandler(registry, pattern, (registration) -> registration.addResourceLocations(locations));
		}

		/**
		 * Adds a resource handler to the given registry with the specified pattern and
		 * customizer. If the registry already has a mapping for the pattern, the method
		 * returns without adding the handler. The customizer is used to configure the
		 * resource handler registration. The cache period, cache control, and use last
		 * modified properties are set based on the resource properties. The resource
		 * handler registration is then customized further.
		 * @param registry the resource handler registry to add the handler to
		 * @param pattern the pattern for the resource handler mapping
		 * @param customizer the customizer to configure the resource handler registration
		 */
		private void addResourceHandler(ResourceHandlerRegistry registry, String pattern,
				Consumer<ResourceHandlerRegistration> customizer) {
			if (registry.hasMappingForPattern(pattern)) {
				return;
			}
			ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
			customizer.accept(registration);
			registration.setCachePeriod(getSeconds(this.resourceProperties.getCache().getPeriod()));
			registration.setCacheControl(this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl());
			registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
			customizeResourceHandlerRegistration(registration);
		}

		/**
		 * Returns the number of seconds in the given cache period.
		 * @param cachePeriod the cache period to get the seconds from
		 * @return the number of seconds in the cache period, or null if the cache period
		 * is null
		 */
		private Integer getSeconds(Duration cachePeriod) {
			return (cachePeriod != null) ? (int) cachePeriod.getSeconds() : null;
		}

		/**
		 * Customizes the registration of the resource handler.
		 * @param registration the resource handler registration to be customized
		 */
		private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
			if (this.resourceHandlerRegistrationCustomizer != null) {
				this.resourceHandlerRegistrationCustomizer.customize(registration);
			}
		}

		/**
		 * Creates and returns a new instance of {@link RequestContextFilter} if no other
		 * beans of type {@link RequestContextListener} or {@link RequestContextFilter}
		 * are present in the application context. If a bean of type
		 * {@link RequestContextFilter} is present, it will not be created. The created
		 * {@link RequestContextFilter} is an instance of
		 * {@link OrderedRequestContextFilter}.
		 * @return the created {@link RequestContextFilter}
		 */
		@Bean
		@ConditionalOnMissingBean({ RequestContextListener.class, RequestContextFilter.class })
		@ConditionalOnMissingFilterBean(RequestContextFilter.class)
		public static RequestContextFilter requestContextFilter() {
			return new OrderedRequestContextFilter();
		}

	}

	/**
	 * Configuration equivalent to {@code @EnableWebMvc}.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebProperties.class)
	public static class EnableWebMvcConfiguration extends DelegatingWebMvcConfiguration implements ResourceLoaderAware {

		private final Resources resourceProperties;

		private final WebMvcProperties mvcProperties;

		private final WebProperties webProperties;

		private final ListableBeanFactory beanFactory;

		private final WebMvcRegistrations mvcRegistrations;

		private ResourceLoader resourceLoader;

		/**
		 * Constructs a new instance of EnableWebMvcConfiguration.
		 * @param mvcProperties the WebMvcProperties object containing the properties for
		 * configuring WebMvc
		 * @param webProperties the WebProperties object containing the properties for
		 * configuring web-related settings
		 * @param mvcRegistrationsProvider the ObjectProvider for WebMvcRegistrations,
		 * used for registering custom WebMvcRegistrations
		 * @param resourceHandlerRegistrationCustomizerProvider the ObjectProvider for
		 * ResourceHandlerRegistrationCustomizer, used for customizing resource handler
		 * registrations
		 * @param beanFactory the ListableBeanFactory used for accessing beans in the
		 * application context
		 */
		public EnableWebMvcConfiguration(WebMvcProperties mvcProperties, WebProperties webProperties,
				ObjectProvider<WebMvcRegistrations> mvcRegistrationsProvider,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
				ListableBeanFactory beanFactory) {
			this.resourceProperties = webProperties.getResources();
			this.mvcProperties = mvcProperties;
			this.webProperties = webProperties;
			this.mvcRegistrations = mvcRegistrationsProvider.getIfUnique();
			this.beanFactory = beanFactory;
		}

		/**
		 * Creates a RequestMappingHandlerAdapter for handling request mappings. If a
		 * custom RequestMappingHandlerAdapter is provided through mvcRegistrations, it
		 * will be used. Otherwise, the default RequestMappingHandlerAdapter will be used.
		 * @return the created RequestMappingHandlerAdapter
		 */
		@Override
		protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
			if (this.mvcRegistrations != null) {
				RequestMappingHandlerAdapter adapter = this.mvcRegistrations.getRequestMappingHandlerAdapter();
				if (adapter != null) {
					return adapter;
				}
			}
			return super.createRequestMappingHandlerAdapter();
		}

		/**
		 * Creates a WelcomePageHandlerMapping bean for handling requests to the welcome
		 * page.
		 * @param applicationContext The application context.
		 * @param mvcConversionService The conversion service for formatting.
		 * @param mvcResourceUrlProvider The resource URL provider for MVC.
		 * @return The created WelcomePageHandlerMapping bean.
		 */
		@Bean
		public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
				FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
			return createWelcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider,
					WelcomePageHandlerMapping::new);
		}

		/**
		 * Creates a WelcomePageNotAcceptableHandlerMapping bean.
		 *
		 * This method is used to create a WelcomePageNotAcceptableHandlerMapping bean,
		 * which is responsible for mapping requests to the welcome page when the
		 * requested media type is not acceptable. It takes the ApplicationContext,
		 * FormattingConversionService, and ResourceUrlProvider as parameters.
		 * @param applicationContext The ApplicationContext used for creating the bean.
		 * @param mvcConversionService The FormattingConversionService used for converting
		 * request parameters.
		 * @param mvcResourceUrlProvider The ResourceUrlProvider used for resolving
		 * resource URLs.
		 * @return The created WelcomePageNotAcceptableHandlerMapping bean.
		 */
		@Bean
		public WelcomePageNotAcceptableHandlerMapping welcomePageNotAcceptableHandlerMapping(
				ApplicationContext applicationContext, FormattingConversionService mvcConversionService,
				ResourceUrlProvider mvcResourceUrlProvider) {
			return createWelcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider,
					WelcomePageNotAcceptableHandlerMapping::new);
		}

		/**
		 * Creates a welcome page handler mapping for the specified application context,
		 * conversion service, resource URL provider, and factory.
		 * @param applicationContext the application context
		 * @param mvcConversionService the formatting conversion service
		 * @param mvcResourceUrlProvider the resource URL provider
		 * @param factory the welcome page handler mapping factory
		 * @param <T> the type of the welcome page handler mapping
		 * @return the created welcome page handler mapping
		 */
		private <T extends AbstractUrlHandlerMapping> T createWelcomePageHandlerMapping(
				ApplicationContext applicationContext, FormattingConversionService mvcConversionService,
				ResourceUrlProvider mvcResourceUrlProvider, WelcomePageHandlerMappingFactory<T> factory) {
			TemplateAvailabilityProviders templateAvailabilityProviders = new TemplateAvailabilityProviders(
					applicationContext);
			String staticPathPattern = this.mvcProperties.getStaticPathPattern();
			T handlerMapping = factory.create(templateAvailabilityProviders, applicationContext, getIndexHtmlResource(),
					staticPathPattern);
			handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
			handlerMapping.setCorsConfigurations(getCorsConfigurations());
			return handlerMapping;
		}

		/**
		 * Returns the locale resolver bean for the web application. If no locale resolver
		 * bean is found, a new one is created based on the configuration properties. If
		 * the locale resolver type is set to FIXED, a FixedLocaleResolver is created with
		 * the specified locale. Otherwise, an AcceptHeaderLocaleResolver is created with
		 * the default locale.
		 * @return the locale resolver bean
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
		public LocaleResolver localeResolver() {
			if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
				return new FixedLocaleResolver(this.webProperties.getLocale());
			}
			AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
			localeResolver.setDefaultLocale(this.webProperties.getLocale());
			return localeResolver;
		}

		/**
		 * Returns the theme resolver bean for the DispatcherServlet.
		 * @return the theme resolver bean
		 * @deprecated since 3.0.0, for removal in future versions
		 *
		 * @see org.springframework.web.servlet.ThemeResolver
		 * @see org.springframework.web.servlet.DispatcherServlet#THEME_RESOLVER_BEAN_NAME
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.THEME_RESOLVER_BEAN_NAME)
		@Deprecated(since = "3.0.0", forRemoval = false)
		@SuppressWarnings("deprecation")
		public org.springframework.web.servlet.ThemeResolver themeResolver() {
			return super.themeResolver();
		}

		/**
		 * Returns the FlashMapManager bean if it is not already defined.
		 * @return the FlashMapManager bean
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
		public FlashMapManager flashMapManager() {
			return super.flashMapManager();
		}

		/**
		 * Retrieves the index.html resource from the specified static locations or
		 * servlet context.
		 * @return The index.html resource, or null if not found.
		 */
		private Resource getIndexHtmlResource() {
			for (String location : this.resourceProperties.getStaticLocations()) {
				Resource indexHtml = getIndexHtmlResource(location);
				if (indexHtml != null) {
					return indexHtml;
				}
			}
			ServletContext servletContext = getServletContext();
			if (servletContext != null) {
				return getIndexHtmlResource(new ServletContextResource(servletContext, SERVLET_LOCATION));
			}
			return null;
		}

		/**
		 * Retrieves the index.html resource from the specified location.
		 * @param location the location of the index.html resource
		 * @return the index.html resource
		 */
		private Resource getIndexHtmlResource(String location) {
			return getIndexHtmlResource(this.resourceLoader.getResource(location));
		}

		/**
		 * Retrieves the index.html resource from the specified location.
		 * @param location the resource location
		 * @return the index.html resource if it exists and has a valid URL, null
		 * otherwise
		 */
		private Resource getIndexHtmlResource(Resource location) {
			try {
				Resource resource = location.createRelative("index.html");
				if (resource.exists() && (resource.getURL() != null)) {
					return resource;
				}
			}
			catch (Exception ex) {
				// Ignore
			}
			return null;
		}

		/**
		 * Returns the formatting conversion service for MVC.
		 *
		 * This method creates a new instance of the {@link WebConversionService} class
		 * and configures it with the specified date, time, and date-time formats obtained
		 * from the {@link Format} object. It then adds any additional formatters
		 * specified by the {@link #addFormatters(WebConversionService)} method.
		 * @return the formatting conversion service for MVC
		 */
		@Bean
		@Override
		public FormattingConversionService mvcConversionService() {
			Format format = this.mvcProperties.getFormat();
			WebConversionService conversionService = new WebConversionService(
					new DateTimeFormatters().dateFormat(format.getDate())
						.timeFormat(format.getTime())
						.dateTimeFormat(format.getDateTime()));
			addFormatters(conversionService);
			return conversionService;
		}

		/**
		 * Returns the MVC validator.
		 *
		 * If the Jakarta Validation API is present in the classpath, it returns a
		 * ValidatorAdapter that wraps the ApplicationContext's validator. Otherwise, it
		 * returns the default MVC validator.
		 * @return the MVC validator
		 */
		@Bean
		@Override
		public Validator mvcValidator() {
			if (!ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
				return super.mvcValidator();
			}
			return ValidatorAdapter.get(getApplicationContext(), getValidator());
		}

		/**
		 * Creates a RequestMappingHandlerMapping for handling request mappings. If a
		 * custom RequestMappingHandlerMapping is provided through mvcRegistrations, it
		 * will be used. Otherwise, the default RequestMappingHandlerMapping will be used.
		 * @return the RequestMappingHandlerMapping instance
		 */
		@Override
		protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
			if (this.mvcRegistrations != null) {
				RequestMappingHandlerMapping mapping = this.mvcRegistrations.getRequestMappingHandlerMapping();
				if (mapping != null) {
					return mapping;
				}
			}
			return super.createRequestMappingHandlerMapping();
		}

		/**
		 * Retrieves the ConfigurableWebBindingInitializer bean from the bean factory, if
		 * available. If the bean is not found, falls back to the super class
		 * implementation.
		 * @param mvcConversionService The FormattingConversionService to be used for data
		 * type conversion during web binding.
		 * @param mvcValidator The Validator to be used for validating web binding data.
		 * @return The ConfigurableWebBindingInitializer bean, if available. Otherwise,
		 * the result of the super class implementation.
		 */
		@Override
		protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
				FormattingConversionService mvcConversionService, Validator mvcValidator) {
			try {
				return this.beanFactory.getBean(ConfigurableWebBindingInitializer.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return super.getConfigurableWebBindingInitializer(mvcConversionService, mvcValidator);
			}
		}

		/**
		 * Creates an instance of ExceptionHandlerExceptionResolver.
		 *
		 * This method is responsible for creating an instance of
		 * ExceptionHandlerExceptionResolver, which is used to handle exceptions thrown
		 * during the processing of HTTP requests.
		 *
		 * If the mvcRegistrations object is not null, it attempts to retrieve the
		 * ExceptionHandlerExceptionResolver instance from it. If found, it returns the
		 * resolver. Otherwise, it calls the super class's
		 * createExceptionHandlerExceptionResolver() method to create the resolver.
		 * @return The ExceptionHandlerExceptionResolver instance.
		 */
		@Override
		protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
			if (this.mvcRegistrations != null) {
				ExceptionHandlerExceptionResolver resolver = this.mvcRegistrations
					.getExceptionHandlerExceptionResolver();
				if (resolver != null) {
					return resolver;
				}
			}
			return super.createExceptionHandlerExceptionResolver();
		}

		/**
		 * Extends the list of handler exception resolvers.
		 * @param exceptionResolvers the list of handler exception resolvers
		 */
		@Override
		protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			super.extendHandlerExceptionResolvers(exceptionResolvers);
			if (this.mvcProperties.isLogResolvedException()) {
				for (HandlerExceptionResolver resolver : exceptionResolvers) {
					if (resolver instanceof AbstractHandlerExceptionResolver abstractResolver) {
						abstractResolver.setWarnLogCategory(resolver.getClass().getName());
					}
				}
			}
		}

		/**
		 * Overrides the mvcContentNegotiationManager method in the
		 * EnableWebMvcConfiguration class. This method creates a new
		 * ContentNegotiationManager and modifies the strategies list by replacing the
		 * PathExtensionContentNegotiationStrategy with an
		 * OptionalPathExtensionContentNegotiationStrategy.
		 * @return the modified ContentNegotiationManager
		 */
		@Bean
		@Override
		@SuppressWarnings("deprecation")
		public ContentNegotiationManager mvcContentNegotiationManager() {
			ContentNegotiationManager manager = super.mvcContentNegotiationManager();
			List<ContentNegotiationStrategy> strategies = manager.getStrategies();
			ListIterator<ContentNegotiationStrategy> iterator = strategies.listIterator();
			while (iterator.hasNext()) {
				ContentNegotiationStrategy strategy = iterator.next();
				if (strategy instanceof org.springframework.web.accept.PathExtensionContentNegotiationStrategy) {
					iterator.set(new OptionalPathExtensionContentNegotiationStrategy(strategy));
				}
			}
			return manager;
		}

		/**
		 * Set the resource loader for this configuration.
		 * @param resourceLoader the resource loader to be set
		 */
		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
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
		 * @param webProperties the WebProperties object containing the configuration for
		 * the web resources
		 * @return a ResourceChainResourceHandlerRegistrationCustomizer object
		 */
		@Bean
		ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
				WebProperties webProperties) {
			return new ResourceChainResourceHandlerRegistrationCustomizer(webProperties.getResources());
		}

	}

	@FunctionalInterface
	interface WelcomePageHandlerMappingFactory<T extends AbstractUrlHandlerMapping> {

		T create(TemplateAvailabilityProviders templateAvailabilityProviders, ApplicationContext applicationContext,
				Resource indexHtmlResource, String staticPathPattern);

	}

	@FunctionalInterface
	interface ResourceHandlerRegistrationCustomizer {

		void customize(ResourceHandlerRegistration registration);

	}

	/**
	 * ResourceChainResourceHandlerRegistrationCustomizer class.
	 */
	static class ResourceChainResourceHandlerRegistrationCustomizer implements ResourceHandlerRegistrationCustomizer {

		private final Resources resourceProperties;

		/**
		 * Constructs a new instance of ResourceChainResourceHandlerRegistrationCustomizer
		 * with the specified resourceProperties.
		 * @param resourceProperties the resource properties to be used by the customizer
		 */
		ResourceChainResourceHandlerRegistrationCustomizer(Resources resourceProperties) {
			this.resourceProperties = resourceProperties;
		}

		/**
		 * Customize the resource handler registration by configuring the resource chain.
		 * @param registration the resource handler registration to customize
		 */
		@Override
		public void customize(ResourceHandlerRegistration registration) {
			Resources.Chain properties = this.resourceProperties.getChain();
			configureResourceChain(properties, registration.resourceChain(properties.isCache()));
		}

		/**
		 * Configures the resource chain for the given properties and chain registration.
		 * @param properties the resource chain properties
		 * @param chain the resource chain registration
		 */
		private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
			Strategy strategy = properties.getStrategy();
			if (properties.isCompressed()) {
				chain.addResolver(new EncodedResourceResolver());
			}
			if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
				chain.addResolver(getVersionResourceResolver(strategy));
			}
		}

		/**
		 * Returns a ResourceResolver based on the provided Strategy properties.
		 * @param properties the Strategy properties to be used for configuring the
		 * ResourceResolver
		 * @return a ResourceResolver object
		 */
		private ResourceResolver getVersionResourceResolver(Strategy properties) {
			VersionResourceResolver resolver = new VersionResourceResolver();
			if (properties.getFixed().isEnabled()) {
				String version = properties.getFixed().getVersion();
				String[] paths = properties.getFixed().getPaths();
				resolver.addFixedVersionStrategy(version, paths);
			}
			if (properties.getContent().isEnabled()) {
				String[] paths = properties.getContent().getPaths();
				resolver.addContentVersionStrategy(paths);
			}
			return resolver;
		}

	}

	/**
	 * ProblemDetailsErrorHandlingConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.mvc.problemdetails", name = "enabled", havingValue = "true")
	static class ProblemDetailsErrorHandlingConfiguration {

		/**
		 * Creates a new instance of ProblemDetailsExceptionHandler if there is no
		 * existing bean of type ResponseEntityExceptionHandler. This handler is
		 * responsible for handling exceptions and returning Problem Details as the
		 * response. The order of this handler is set to 0, indicating that it should be
		 * executed first if multiple exception handlers are present.
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
	 * Decorator to make
	 * {@link org.springframework.web.accept.PathExtensionContentNegotiationStrategy}
	 * optional depending on a request attribute.
	 */
	static class OptionalPathExtensionContentNegotiationStrategy implements ContentNegotiationStrategy {

		@SuppressWarnings("deprecation")
		private static final String SKIP_ATTRIBUTE = org.springframework.web.accept.PathExtensionContentNegotiationStrategy.class
			.getName() + ".SKIP";

		private final ContentNegotiationStrategy delegate;

		/**
		 * Constructs a new OptionalPathExtensionContentNegotiationStrategy with the
		 * specified delegate.
		 * @param delegate the delegate ContentNegotiationStrategy to be used
		 */
		OptionalPathExtensionContentNegotiationStrategy(ContentNegotiationStrategy delegate) {
			this.delegate = delegate;
		}

		/**
		 * Resolves the media types for the given web request.
		 * @param webRequest the current web request
		 * @return the list of resolved media types
		 * @throws HttpMediaTypeNotAcceptableException if the media types cannot be
		 * resolved
		 */
		@Override
		public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
				throws HttpMediaTypeNotAcceptableException {
			Object skip = webRequest.getAttribute(SKIP_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
			if (skip != null && Boolean.parseBoolean(skip.toString())) {
				return MEDIA_TYPE_ALL_LIST;
			}
			return this.delegate.resolveMediaTypes(webRequest);
		}

	}

}
