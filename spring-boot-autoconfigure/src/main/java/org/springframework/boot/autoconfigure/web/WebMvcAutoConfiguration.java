/*
 * Copyright 2012-2013 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebMvc Web MVC}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class,
		WebMvcConfigurerAdapter.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter(DispatcherServletAutoConfiguration.class)
public class WebMvcAutoConfiguration {

	private static final String[] SERVLET_RESOURCE_LOCATIONS = { "/" };

	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
			"classpath:/META-INF/resources/", "classpath:/resources/",
			"classpath:/static/", "classpath:/public/" };

	private static final String[] RESOURCE_LOCATIONS;
	static {
		RESOURCE_LOCATIONS = new String[CLASSPATH_RESOURCE_LOCATIONS.length
				+ SERVLET_RESOURCE_LOCATIONS.length];
		System.arraycopy(SERVLET_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS, 0,
				SERVLET_RESOURCE_LOCATIONS.length);
		System.arraycopy(CLASSPATH_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS,
				SERVLET_RESOURCE_LOCATIONS.length, CLASSPATH_RESOURCE_LOCATIONS.length);
	}

	private static final String[] STATIC_INDEX_HTML_RESOURCES;
	static {
		STATIC_INDEX_HTML_RESOURCES = new String[RESOURCE_LOCATIONS.length];
		for (int i = 0; i < STATIC_INDEX_HTML_RESOURCES.length; i++) {
			STATIC_INDEX_HTML_RESOURCES[i] = RESOURCE_LOCATIONS[i] + "index.html";
		}
	}

	public static String DEFAULT_PREFIX = "";
	public static String DEFAULT_SUFFIX = "";

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new HiddenHttpMethodFilter();
	}

	public static boolean templateExists(Environment environment,
			ResourceLoader resourceLoader, String view) {
		String prefix = environment.getProperty("spring.view.prefix",
				WebMvcAutoConfiguration.DEFAULT_PREFIX);
		String suffix = environment.getProperty("spring.view.suffix",
				WebMvcAutoConfiguration.DEFAULT_SUFFIX);
		return resourceLoader.getResource(prefix + view + suffix).exists();
	}

	// Defined as a nested config to ensure WebMvcConfigurerAdapter it not read when not
	// on the classpath
	@EnableWebMvc
	public static class WebMvcAutoConfigurationAdapter extends WebMvcConfigurerAdapter {

		private static Log logger = LogFactory.getLog(WebMvcConfigurerAdapter.class);

		@Value("${spring.view.prefix:}")
		private String prefix = "";

		@Value("${spring.view.suffix:}")
		private String suffix = "";

		@Value("${spring.resources.cachePeriod:}")
		private Integer cachePeriod;

		@Autowired
		private ListableBeanFactory beanFactory;

		@Autowired
		private ResourceLoader resourceLoader;

		@Autowired
		private HttpMessageConverters messageConverters;

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.addAll(this.messageConverters.getConverters());
		}

		@Bean
		@ConditionalOnMissingBean(InternalResourceViewResolver.class)
		public InternalResourceViewResolver defaultViewResolver() {
			InternalResourceViewResolver resolver = new InternalResourceViewResolver();
			resolver.setPrefix(this.prefix);
			resolver.setSuffix(this.suffix);
			return resolver;
		}

		@Bean
		@ConditionalOnMissingBean(RequestContextListener.class)
		public RequestContextListener requestContextListener() {
			return new RequestContextListener();
		}

		@Bean
		@ConditionalOnBean(View.class)
		public BeanNameViewResolver beanNameViewResolver() {
			BeanNameViewResolver resolver = new BeanNameViewResolver();
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
			return resolver;
		}

		@Bean
		@ConditionalOnBean(View.class)
		public ContentNegotiatingViewResolver viewResolver(BeanFactory beanFactory) {
			ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
			resolver.setContentNegotiationManager(beanFactory
					.getBean(ContentNegotiationManager.class));
			// ContentNegotiatingViewResolver uses all the other view resolvers to locate
			// a view so it should have a high precedence
			resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return resolver;
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			for (Converter<?, ?> converter : getBeansOfType(Converter.class)) {
				registry.addConverter(converter);
			}

			for (GenericConverter converter : getBeansOfType(GenericConverter.class)) {
				registry.addConverter(converter);
			}

			for (Formatter<?> formatter : getBeansOfType(Formatter.class)) {
				registry.addFormatter(formatter);
			}
		}

		private <T> Collection<T> getBeansOfType(Class<T> type) {
			return this.beanFactory.getBeansOfType(type).values();
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (!registry.hasMappingForPattern("/webjars/**")) {
				registry.addResourceHandler("/webjars/**")
						.addResourceLocations("classpath:/META-INF/resources/webjars/")
						.setCachePeriod(this.cachePeriod);
			}
			if (!registry.hasMappingForPattern("/**")) {
				registry.addResourceHandler("/**")
						.addResourceLocations(RESOURCE_LOCATIONS)
						.setCachePeriod(this.cachePeriod);
			}
		}

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			addStaticIndexHtmlViewControllers(registry);
		}

		private void addStaticIndexHtmlViewControllers(ViewControllerRegistry registry) {
			for (String resource : STATIC_INDEX_HTML_RESOURCES) {
				if (this.resourceLoader.getResource(resource).exists()) {
					try {
						logger.info("Adding welcome page: "
								+ this.resourceLoader.getResource(resource).getURL());
					}
					catch (IOException ex) {
						// Ignore
					}
					registry.addViewController("/").setViewName("/index.html");
					return;
				}
			}
		}

		@Configuration
		public static class FaviconConfiguration {

			@Bean
			public SimpleUrlHandlerMapping faviconHandlerMapping() {
				SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
				mapping.setOrder(Integer.MIN_VALUE + 1);
				mapping.setUrlMap(Collections.singletonMap("**/favicon.ico",
						faviconRequestHandler()));
				return mapping;
			}

			@Bean
			protected ResourceHttpRequestHandler faviconRequestHandler() {
				ResourceHttpRequestHandler requestHandler = new ResourceHttpRequestHandler();
				requestHandler.setLocations(Arrays
						.<Resource> asList(new ClassPathResource("/")));
				return requestHandler;
			}
		}

	}

}
