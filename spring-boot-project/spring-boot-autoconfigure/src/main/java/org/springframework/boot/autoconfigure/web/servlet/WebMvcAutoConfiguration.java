/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
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
import org.springframework.web.servlet.ThemeResolver;
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
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

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
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter({ DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class,
		ValidationAutoConfiguration.class })
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

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnProperty(prefix = "spring.mvc.hiddenmethod.filter", name = "enabled")
	public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	@Bean
	@ConditionalOnMissingBean(FormContentFilter.class)
	@ConditionalOnProperty(prefix = "spring.mvc.formcontent.filter", name = "enabled", matchIfMissing = true)
	public OrderedFormContentFilter formContentFilter() {
		return new OrderedFormContentFilter();
	}

	// Defined as a nested config to ensure WebMvcConfigurer is not read when not
	// on the classpath
	@SuppressWarnings("deprecation")
	@Configuration(proxyBeanMethods = false)
	@Import(EnableWebMvcConfiguration.class)
	@EnableConfigurationProperties({ WebMvcProperties.class,
			org.springframework.boot.autoconfigure.web.ResourceProperties.class, WebProperties.class })
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

		public WebMvcAutoConfigurationAdapter(
				org.springframework.boot.autoconfigure.web.ResourceProperties resourceProperties,
				WebProperties webProperties, WebMvcProperties mvcProperties, ListableBeanFactory beanFactory,
				ObjectProvider<HttpMessageConverters> messageConvertersProvider,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
				ObjectProvider<DispatcherServletPath> dispatcherServletPath,
				ObjectProvider<ServletRegistrationBean<?>> servletRegistrations) {
			this.resourceProperties = resourceProperties.hasBeenCustomized() ? resourceProperties
					: webProperties.getResources();
			this.mvcProperties = mvcProperties;
			this.beanFactory = beanFactory;
			this.messageConvertersProvider = messageConvertersProvider;
			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizerProvider.getIfAvailable();
			this.dispatcherServletPath = dispatcherServletPath;
			this.servletRegistrations = servletRegistrations;
			this.mvcProperties.checkConfiguration();
		}

		@Override
		public void setServletContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			this.messageConvertersProvider
					.ifAvailable((customConverters) -> converters.addAll(customConverters.getConverters()));
		}

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			if (this.beanFactory.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)) {
				Object taskExecutor = this.beanFactory
						.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
				if (taskExecutor instanceof AsyncTaskExecutor) {
					configurer.setTaskExecutor(((AsyncTaskExecutor) taskExecutor));
				}
			}
			Duration timeout = this.mvcProperties.getAsync().getRequestTimeout();
			if (timeout != null) {
				configurer.setDefaultTimeout(timeout.toMillis());
			}
		}

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			if (this.mvcProperties.getPathmatch()
					.getMatchingStrategy() == WebMvcProperties.MatchingStrategy.PATH_PATTERN_PARSER) {
				configurer.setPatternParser(new PathPatternParser());
			}
			configurer.setUseSuffixPatternMatch(this.mvcProperties.getPathmatch().isUseSuffixPattern());
			configurer.setUseRegisteredSuffixPatternMatch(
					this.mvcProperties.getPathmatch().isUseRegisteredSuffixPattern());
			this.dispatcherServletPath.ifAvailable((dispatcherPath) -> {
				String servletUrlMapping = dispatcherPath.getServletUrlMapping();
				if (servletUrlMapping.equals("/") && singleDispatcherServlet()) {
					UrlPathHelper urlPathHelper = new UrlPathHelper();
					urlPathHelper.setAlwaysUseFullPath(true);
					configurer.setUrlPathHelper(urlPathHelper);
				}
			});
		}

		private boolean singleDispatcherServlet() {
			return this.servletRegistrations.stream().map(ServletRegistrationBean::getServlet)
					.filter(DispatcherServlet.class::isInstance).count() == 1;
		}

		@Override
		public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
			WebMvcProperties.Contentnegotiation contentnegotiation = this.mvcProperties.getContentnegotiation();
			configurer.favorPathExtension(contentnegotiation.isFavorPathExtension());
			configurer.favorParameter(contentnegotiation.isFavorParameter());
			if (contentnegotiation.getParameterName() != null) {
				configurer.parameterName(contentnegotiation.getParameterName());
			}
			Map<String, MediaType> mediaTypes = this.mvcProperties.getContentnegotiation().getMediaTypes();
			mediaTypes.forEach(configurer::mediaType);
		}

		@Bean
		@ConditionalOnMissingBean
		public InternalResourceViewResolver defaultViewResolver() {
			InternalResourceViewResolver resolver = new InternalResourceViewResolver();
			resolver.setPrefix(this.mvcProperties.getView().getPrefix());
			resolver.setSuffix(this.mvcProperties.getView().getSuffix());
			return resolver;
		}

		@Bean
		@ConditionalOnBean(View.class)
		@ConditionalOnMissingBean
		public BeanNameViewResolver beanNameViewResolver() {
			BeanNameViewResolver resolver = new BeanNameViewResolver();
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
			return resolver;
		}

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

		@Override
		public MessageCodesResolver getMessageCodesResolver() {
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
			addResourceHandler(registry, "/webjars/**", "classpath:/META-INF/resources/webjars/");
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
			registration.setCachePeriod(getSeconds(this.resourceProperties.getCache().getPeriod()));
			registration.setCacheControl(this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl());
			registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
			customizeResourceHandlerRegistration(registration);
		}

		private Integer getSeconds(Duration cachePeriod) {
			return (cachePeriod != null) ? (int) cachePeriod.getSeconds() : null;
		}

		private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
			if (this.resourceHandlerRegistrationCustomizer != null) {
				this.resourceHandlerRegistrationCustomizer.customize(registration);
			}
		}

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

		@SuppressWarnings("deprecation")
		public EnableWebMvcConfiguration(
				org.springframework.boot.autoconfigure.web.ResourceProperties resourceProperties,
				WebMvcProperties mvcProperties, WebProperties webProperties,
				ObjectProvider<WebMvcRegistrations> mvcRegistrationsProvider,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
				ListableBeanFactory beanFactory) {
			this.resourceProperties = resourceProperties.hasBeenCustomized() ? resourceProperties
					: webProperties.getResources();
			this.mvcProperties = mvcProperties;
			this.webProperties = webProperties;
			this.mvcRegistrations = mvcRegistrationsProvider.getIfUnique();
			this.beanFactory = beanFactory;
		}

		@Bean
		@Override
		public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
				@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
				@Qualifier("mvcConversionService") FormattingConversionService conversionService,
				@Qualifier("mvcValidator") Validator validator) {
			RequestMappingHandlerAdapter adapter = super.requestMappingHandlerAdapter(contentNegotiationManager,
					conversionService, validator);
			adapter.setIgnoreDefaultModelOnRedirect(
					this.mvcProperties == null || this.mvcProperties.isIgnoreDefaultModelOnRedirect());
			return adapter;
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
		@Primary
		@Override
		public RequestMappingHandlerMapping requestMappingHandlerMapping(
				@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
				@Qualifier("mvcConversionService") FormattingConversionService conversionService,
				@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
			// Must be @Primary for MvcUriComponentsBuilder to work
			return super.requestMappingHandlerMapping(contentNegotiationManager, conversionService,
					resourceUrlProvider);
		}

		@Bean
		public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
				FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
			WelcomePageHandlerMapping welcomePageHandlerMapping = new WelcomePageHandlerMapping(
					new TemplateAvailabilityProviders(applicationContext), applicationContext, getWelcomePage(),
					this.mvcProperties.getStaticPathPattern());
			welcomePageHandlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
			welcomePageHandlerMapping.setCorsConfigurations(getCorsConfigurations());
			return welcomePageHandlerMapping;
		}

		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
		@SuppressWarnings("deprecation")
		public LocaleResolver localeResolver() {
			if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
				return new FixedLocaleResolver(this.webProperties.getLocale());
			}
			if (this.mvcProperties.getLocaleResolver() == WebMvcProperties.LocaleResolver.FIXED) {
				return new FixedLocaleResolver(this.mvcProperties.getLocale());
			}
			AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
			Locale locale = (this.webProperties.getLocale() != null) ? this.webProperties.getLocale()
					: this.mvcProperties.getLocale();
			localeResolver.setDefaultLocale(locale);
			return localeResolver;
		}

		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.THEME_RESOLVER_BEAN_NAME)
		public ThemeResolver themeResolver() {
			return super.themeResolver();
		}

		@Override
		@Bean
		@ConditionalOnMissingBean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
		public FlashMapManager flashMapManager() {
			return super.flashMapManager();
		}

		private Resource getWelcomePage() {
			for (String location : this.resourceProperties.getStaticLocations()) {
				Resource indexHtml = getIndexHtml(location);
				if (indexHtml != null) {
					return indexHtml;
				}
			}
			ServletContext servletContext = getServletContext();
			if (servletContext != null) {
				return getIndexHtml(new ServletContextResource(servletContext, SERVLET_LOCATION));
			}
			return null;
		}

		private Resource getIndexHtml(String location) {
			return getIndexHtml(this.resourceLoader.getResource(location));
		}

		private Resource getIndexHtml(Resource location) {
			try {
				Resource resource = location.createRelative("index.html");
				if (resource.exists() && (resource.getURL() != null)) {
					return resource;
				}
			}
			catch (Exception ex) {
			}
			return null;
		}

		@Bean
		@Override
		public FormattingConversionService mvcConversionService() {
			Format format = this.mvcProperties.getFormat();
			WebConversionService conversionService = new WebConversionService(new DateTimeFormatters()
					.dateFormat(format.getDate()).timeFormat(format.getTime()).dateTimeFormat(format.getDateTime()));
			addFormatters(conversionService);
			return conversionService;
		}

		@Bean
		@Override
		public Validator mvcValidator() {
			if (!ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				return super.mvcValidator();
			}
			return ValidatorAdapter.get(getApplicationContext(), getValidator());
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
					if (resolver instanceof AbstractHandlerExceptionResolver) {
						((AbstractHandlerExceptionResolver) resolver).setWarnLogCategory(resolver.getClass().getName());
					}
				}
			}
		}

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

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnEnabledResourceChain
	static class ResourceChainCustomizerConfiguration {

		@Bean
		@SuppressWarnings("deprecation")
		ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
				org.springframework.boot.autoconfigure.web.ResourceProperties resourceProperties,
				WebProperties webProperties) {
			return new ResourceChainResourceHandlerRegistrationCustomizer(
					resourceProperties.hasBeenCustomized() ? resourceProperties : webProperties.getResources());
		}

	}

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

		@SuppressWarnings("deprecation")
		private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
			Strategy strategy = properties.getStrategy();
			if (properties.isCompressed()) {
				chain.addResolver(new EncodedResourceResolver());
			}
			if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
				chain.addResolver(getVersionResourceResolver(strategy));
			}
			if (properties instanceof org.springframework.boot.autoconfigure.web.ResourceProperties.Chain
					&& ((org.springframework.boot.autoconfigure.web.ResourceProperties.Chain) properties)
							.isHtmlApplicationCache()) {
				chain.addTransformer(new org.springframework.web.servlet.resource.AppCacheManifestTransformer());
			}
		}

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
	 * Decorator to make
	 * {@link org.springframework.web.accept.PathExtensionContentNegotiationStrategy}
	 * optional depending on a request attribute.
	 */
	static class OptionalPathExtensionContentNegotiationStrategy implements ContentNegotiationStrategy {

		@SuppressWarnings("deprecation")
		private static final String SKIP_ATTRIBUTE = org.springframework.web.accept.PathExtensionContentNegotiationStrategy.class
				.getName() + ".SKIP";

		private final ContentNegotiationStrategy delegate;

		OptionalPathExtensionContentNegotiationStrategy(ContentNegotiationStrategy delegate) {
			this.delegate = delegate;
		}

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
