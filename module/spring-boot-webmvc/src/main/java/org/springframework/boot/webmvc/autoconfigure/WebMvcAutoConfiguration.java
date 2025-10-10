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

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources.Chain.Strategy;
import org.springframework.boot.autoconfigure.web.WebResourcesRuntimeHints;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.servlet.filter.OrderedFormContentFilter;
import org.springframework.boot.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.boot.servlet.filter.OrderedRequestContextFilter;
import org.springframework.boot.validation.autoconfigure.ValidatorAdapter;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Apiversion;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Apiversion.Use;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Format;
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
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.lang.Contract;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
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
 * @since 4.0.0
 */
@AutoConfiguration(after = { DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class },
		afterName = "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@ImportRuntimeHints(WebResourcesRuntimeHints.class)
public final class WebMvcAutoConfiguration {

	/**
	 * The default Spring MVC view prefix.
	 */
	public static final String DEFAULT_PREFIX = "";

	/**
	 * The default Spring MVC view suffix.
	 */
	public static final String DEFAULT_SUFFIX = "";

	private static final String SERVLET_LOCATION = "/";

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnBooleanProperty("spring.mvc.hiddenmethod.filter.enabled")
	OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	@Bean
	@ConditionalOnMissingBean(FormContentFilter.class)
	@ConditionalOnBooleanProperty(name = "spring.mvc.formcontent.filter.enabled", matchIfMissing = true)
	OrderedFormContentFilter formContentFilter() {
		return new OrderedFormContentFilter();
	}

	// Defined as a nested config to ensure WebMvcConfigurer is not read when not
	// on the classpath
	@Configuration(proxyBeanMethods = false)
	@Import(EnableWebMvcConfiguration.class)
	@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
	@Order(0)
	static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer, ServletContextAware {

		private static final Log logger = LogFactory.getLog(WebMvcConfigurer.class);

		private final Resources resourceProperties;

		private final WebMvcProperties mvcProperties;

		private final ListableBeanFactory beanFactory;

		private final ObjectProvider<DispatcherServletPath> dispatcherServletPath;

		private final ObjectProvider<ServletRegistrationBean<?>> servletRegistrations;

		private final ObjectProvider<ServerHttpMessageConvertersCustomizer> httpMessageConvertersCustomizerProvider;

		private final @Nullable ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;

		private @Nullable ServletContext servletContext;

		private final ObjectProvider<ApiVersionResolver> apiVersionResolvers;

		private final ObjectProvider<ApiVersionParser<?>> apiVersionParser;

		private final ObjectProvider<ApiVersionDeprecationHandler> apiVersionDeprecationHandler;

		WebMvcAutoConfigurationAdapter(WebProperties webProperties, WebMvcProperties mvcProperties,
				ListableBeanFactory beanFactory,
				ObjectProvider<ServerHttpMessageConvertersCustomizer> httpMessageConvertersCustomizerProvider,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
				ObjectProvider<DispatcherServletPath> dispatcherServletPath,
				ObjectProvider<ServletRegistrationBean<?>> servletRegistrations,
				ObjectProvider<ApiVersionResolver> apiVersionResolvers,
				ObjectProvider<ApiVersionParser<?>> apiVersionParser,
				ObjectProvider<ApiVersionDeprecationHandler> apiVersionDeprecationHandler) {
			this.resourceProperties = webProperties.getResources();
			this.mvcProperties = mvcProperties;
			this.beanFactory = beanFactory;
			this.httpMessageConvertersCustomizerProvider = httpMessageConvertersCustomizerProvider;
			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizerProvider.getIfAvailable();
			this.dispatcherServletPath = dispatcherServletPath;
			this.servletRegistrations = servletRegistrations;
			this.apiVersionResolvers = apiVersionResolvers;
			this.apiVersionParser = apiVersionParser;
			this.apiVersionDeprecationHandler = apiVersionDeprecationHandler;
		}

		@Override
		public void setServletContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void configureMessageConverters(ServerBuilder builder) {
			this.httpMessageConvertersCustomizerProvider.forEach((customizer) -> customizer.customize(builder));
		}

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

		@Override
		@SuppressWarnings("removal")
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

		private boolean singleDispatcherServlet() {
			return this.servletRegistrations.stream()
				.map(ServletRegistrationBean::getServlet)
				.filter(DispatcherServlet.class::isInstance)
				.count() == 1;
		}

		@Override
		public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
			WebMvcProperties.Contentnegotiation contentnegotiation = this.mvcProperties.getContentnegotiation();
			configurer.favorParameter(contentnegotiation.isFavorParameter());
			if (contentnegotiation.getParameterName() != null) {
				configurer.parameterName(contentnegotiation.getParameterName());
			}
			Map<String, MediaType> mediaTypes = contentnegotiation.getMediaTypes();
			mediaTypes.forEach(configurer::mediaType);
			List<MediaType> defaultContentTypes = contentnegotiation.getDefaultContentTypes();
			if (!CollectionUtils.isEmpty(defaultContentTypes)) {
				configurer.defaultContentType(defaultContentTypes.toArray(new MediaType[0]));
			}
		}

		@Bean
		@ConditionalOnMissingBean
		InternalResourceViewResolver defaultViewResolver() {
			InternalResourceViewResolver resolver = new InternalResourceViewResolver();
			resolver.setPrefix(this.mvcProperties.getView().getPrefix());
			resolver.setSuffix(this.mvcProperties.getView().getSuffix());
			return resolver;
		}

		@Bean
		@ConditionalOnBean(View.class)
		@ConditionalOnMissingBean
		BeanNameViewResolver beanNameViewResolver() {
			BeanNameViewResolver resolver = new BeanNameViewResolver();
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
			return resolver;
		}

		@Bean
		@ConditionalOnBean(ViewResolver.class)
		@ConditionalOnMissingBean(name = "viewResolver", value = ContentNegotiatingViewResolver.class)
		ContentNegotiatingViewResolver viewResolver(BeanFactory beanFactory) {
			ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
			resolver.setContentNegotiationManager(beanFactory.getBean(ContentNegotiationManager.class));
			// ContentNegotiatingViewResolver uses all the other view resolvers to locate
			// a view so it should have a high precedence
			resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return resolver;
		}

		@Override
		public @Nullable MessageCodesResolver getMessageCodesResolver() {
			if (this.mvcProperties.getMessageCodesResolverFormat() != null) {
				DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();
				resolver.setMessageCodeFormatter(this.mvcProperties.getMessageCodesResolverFormat());
				return resolver;
			}
			return null;
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			ApplicationConversionService.addBeans(registry, this.beanFactory);
		}

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

		private void addResourceHandler(ResourceHandlerRegistry registry, String pattern, String... locations) {
			addResourceHandler(registry, pattern, (registration) -> registration.addResourceLocations(locations));
		}

		private void addResourceHandler(ResourceHandlerRegistry registry, String pattern,
				Consumer<ResourceHandlerRegistration> customizer) {
			if (registry.hasMappingForPattern(pattern)) {
				return;
			}
			ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
			customizer.accept(registration);
			Integer cachePeriod = getSeconds(this.resourceProperties.getCache().getPeriod());
			if (cachePeriod != null) {
				registration.setCachePeriod(cachePeriod);
			}
			CacheControl cacheControl = this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl();
			if (cacheControl != null) {
				registration.setCacheControl(cacheControl);
			}
			registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
			customizeResourceHandlerRegistration(registration);
		}

		@Contract("!null -> !null")
		private @Nullable Integer getSeconds(@Nullable Duration cachePeriod) {
			return (cachePeriod != null) ? (int) cachePeriod.getSeconds() : null;
		}

		private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
			if (this.resourceHandlerRegistrationCustomizer != null) {
				this.resourceHandlerRegistrationCustomizer.customize(registration);
			}
		}

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {
			PropertyMapper map = PropertyMapper.get();
			Apiversion properties = this.mvcProperties.getApiversion();
			map.from(properties::getRequired).to(configurer::setVersionRequired);
			map.from(properties::getDefaultVersion).to(configurer::setDefaultVersion);
			map.from(properties::getSupported).to((supported) -> supported.forEach(configurer::addSupportedVersions));
			map.from(properties::getDetectSupported).to(configurer::detectSupportedVersions);
			configureApiVersioningUse(configurer, properties.getUse());
			this.apiVersionResolvers.orderedStream().forEach(configurer::useVersionResolver);
			this.apiVersionParser.ifAvailable(configurer::setVersionParser);
			this.apiVersionDeprecationHandler.ifAvailable(configurer::setDeprecationHandler);
		}

		private void configureApiVersioningUse(ApiVersionConfigurer configurer, Use use) {
			PropertyMapper map = PropertyMapper.get();
			map.from(use::getHeader).whenHasText().to(configurer::useRequestHeader);
			map.from(use::getQueryParameter).whenHasText().to(configurer::useQueryParam);
			map.from(use::getPathSegment).to(configurer::usePathSegment);
			use.getMediaTypeParameter()
				.forEach((mediaType, parameterName) -> configurer.useMediaTypeParameter(mediaType, parameterName));
		}

		@Bean
		@ConditionalOnMissingBean({ RequestContextListener.class, RequestContextFilter.class })
		@ConditionalOnMissingFilterBean
		static RequestContextFilter requestContextFilter() {
			return new OrderedRequestContextFilter();
		}

	}

	/**
	 * Configuration equivalent to {@code @EnableWebMvc}.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebProperties.class)
	static class EnableWebMvcConfiguration extends DelegatingWebMvcConfiguration implements ResourceLoaderAware {

		private final Resources resourceProperties;

		private final WebMvcProperties mvcProperties;

		private final WebProperties webProperties;

		private final ListableBeanFactory beanFactory;

		private final @Nullable WebMvcRegistrations mvcRegistrations;

		@SuppressWarnings("NullAway.Init")
		private ResourceLoader resourceLoader;

		EnableWebMvcConfiguration(WebMvcProperties mvcProperties, WebProperties webProperties,
				ObjectProvider<WebMvcRegistrations> mvcRegistrationsProvider,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
				ListableBeanFactory beanFactory) {
			this.resourceProperties = webProperties.getResources();
			this.mvcProperties = mvcProperties;
			this.webProperties = webProperties;
			this.mvcRegistrations = mvcRegistrationsProvider.getIfUnique();
			this.beanFactory = beanFactory;
		}

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

		@Bean
		WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
				FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
			return createWelcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider,
					WelcomePageHandlerMapping::new);
		}

		@Bean
		WelcomePageNotAcceptableHandlerMapping welcomePageNotAcceptableHandlerMapping(
				ApplicationContext applicationContext, FormattingConversionService mvcConversionService,
				ResourceUrlProvider mvcResourceUrlProvider) {
			return createWelcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider,
					WelcomePageNotAcceptableHandlerMapping::new);
		}

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

		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
		public LocaleResolver localeResolver() {
			Locale locale = this.webProperties.getLocale();
			if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
				Assert.state(locale != null, "'locale' must not be null");
				return new FixedLocaleResolver(locale);
			}
			AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
			localeResolver.setDefaultLocale(locale);
			return localeResolver;
		}

		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
		public FlashMapManager flashMapManager() {
			return super.flashMapManager();
		}

		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME)
		public RequestToViewNameTranslator viewNameTranslator() {
			return super.viewNameTranslator();
		}

		private @Nullable Resource getIndexHtmlResource() {
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

		private @Nullable Resource getIndexHtmlResource(String location) {
			return getIndexHtmlResource(this.resourceLoader.getResource(location));
		}

		private @Nullable Resource getIndexHtmlResource(Resource location) {
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

		@Bean
		@Override
		public Validator mvcValidator() {
			if (!ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())
					|| !ClassUtils.isPresent("org.springframework.boot.validation.autoconfigure.ValidatorAdapter",
							getClass().getClassLoader())) {
				return super.mvcValidator();
			}
			ApplicationContext applicationContext = getApplicationContext();
			Assert.state(applicationContext != null, "'applicationContext' must not be null");
			return ValidatorAdapter.get(applicationContext, getValidator());
		}

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

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		@Override
		@ConditionalOnMissingBean(name = "mvcApiVersionStrategy")
		public @Nullable ApiVersionStrategy mvcApiVersionStrategy() {
			return super.mvcApiVersionStrategy();
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

	@FunctionalInterface
	interface WelcomePageHandlerMappingFactory<T extends AbstractUrlHandlerMapping> {

		T create(TemplateAvailabilityProviders templateAvailabilityProviders, ApplicationContext applicationContext,
				@Nullable Resource indexHtmlResource, String staticPathPattern);

	}

	@FunctionalInterface
	interface ResourceHandlerRegistrationCustomizer {

		void customize(ResourceHandlerRegistration registration);

	}

	static class ResourceChainResourceHandlerRegistrationCustomizer implements ResourceHandlerRegistrationCustomizer {

		private final Resources resourceProperties;

		ResourceChainResourceHandlerRegistrationCustomizer(Resources resourceProperties) {
			this.resourceProperties = resourceProperties;
		}

		@Override
		public void customize(ResourceHandlerRegistration registration) {
			Resources.Chain properties = this.resourceProperties.getChain();
			configureResourceChain(properties, registration.resourceChain(properties.isCache()));
		}

		private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
			Strategy strategy = properties.getStrategy();
			if (properties.isCompressed()) {
				chain.addResolver(new EncodedResourceResolver());
			}
			if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
				chain.addResolver(getVersionResourceResolver(strategy));
			}
		}

		private ResourceResolver getVersionResourceResolver(Strategy properties) {
			VersionResourceResolver resolver = new VersionResourceResolver();
			if (properties.getFixed().isEnabled()) {
				String version = properties.getFixed().getVersion();
				String[] paths = properties.getFixed().getPaths();
				Assert.state(version != null, "'version' must not be null");
				resolver.addFixedVersionStrategy(version, paths);
			}
			if (properties.getContent().isEnabled()) {
				String[] paths = properties.getContent().getPaths();
				resolver.addContentVersionStrategy(paths);
			}
			return resolver;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty("spring.mvc.problemdetails.enabled")
	static class ProblemDetailsErrorHandlingConfiguration {

		@Bean
		@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
		@Order(0)
		ProblemDetailsExceptionHandler problemDetailsExceptionHandler() {
			return new ProblemDetailsExceptionHandler();
		}

	}

}
