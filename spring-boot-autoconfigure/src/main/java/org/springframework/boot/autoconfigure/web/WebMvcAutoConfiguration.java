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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Servlet;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.condition.ConditionalOnBean;
import org.springframework.boot.context.condition.ConditionalOnClass;
import org.springframework.boot.context.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebMvc Web MVC}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class,
		WebMvcConfigurerAdapter.class })
@ConditionalOnMissingBean({ HandlerAdapter.class })
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
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
		STATIC_INDEX_HTML_RESOURCES = new String[CLASSPATH_RESOURCE_LOCATIONS.length];
		for (int i = 0; i < STATIC_INDEX_HTML_RESOURCES.length; i++) {
			STATIC_INDEX_HTML_RESOURCES[i] = CLASSPATH_RESOURCE_LOCATIONS[i]
					+ "index.html";
		}
	}

	// Defined as a nested config to ensure WebMvcConfigurerAdapter it not read when not
	// on the classpath
	@EnableWebMvc
	public static class WebMvcAutoConfigurationAdapter extends WebMvcConfigurerAdapter {

		@Autowired
		private ListableBeanFactory beanFactory;

		@Autowired
		private ResourceLoader resourceLoader;

		@ConditionalOnBean(View.class)
		@Bean
		public BeanNameViewResolver beanNameViewResolver() {
			BeanNameViewResolver resolver = new BeanNameViewResolver();
			resolver.setOrder(0);
			return resolver;
		}

		@ConditionalOnBean(View.class)
		@Bean
		public ContentNegotiatingViewResolver viewResolver(BeanFactory beanFactory) {
			ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
			resolver.setContentNegotiationManager(beanFactory
					.getBean(ContentNegotiationManager.class));
			return resolver;
		}

		@Override
		public void configureDefaultServletHandling(
				DefaultServletHandlerConfigurer configurer) {
			configurer.enable();
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
			registry.addResourceHandler("/resources/**").addResourceLocations(
					RESOURCE_LOCATIONS);
			registry.addResourceHandler("/**").addResourceLocations(RESOURCE_LOCATIONS);
		}

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			addStaticIndexHtmlViewControllers(registry);
		}

		private void addStaticIndexHtmlViewControllers(ViewControllerRegistry registry) {
			for (String resource : STATIC_INDEX_HTML_RESOURCES) {
				if (this.resourceLoader.getResource(resource).exists()) {
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

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new HiddenHttpMethodFilter();
	}

}
