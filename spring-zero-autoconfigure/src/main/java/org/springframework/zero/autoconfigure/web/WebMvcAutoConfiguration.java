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

package org.springframework.zero.autoconfigure.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Servlet;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.zero.context.annotation.AutoConfigureAfter;
import org.springframework.zero.context.annotation.ConditionalOnBean;
import org.springframework.zero.context.annotation.ConditionalOnClass;
import org.springframework.zero.context.annotation.ConditionalOnMissingBean;
import org.springframework.zero.context.annotation.EnableAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebMvc Web MVC}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class,
		WebMvcConfigurerAdapter.class })
@ConditionalOnMissingBean({ HandlerAdapter.class, HandlerMapping.class })
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
public class WebMvcAutoConfiguration {

	// Defined as a nested config to ensure WebMvcConfigurerAdapter it not read when not
	// on the classpath
	@EnableWebMvc
	public static class WebMvcAutoConfigurationAdapter extends WebMvcConfigurerAdapter {

		@Autowired
		private ListableBeanFactory beanFactory;

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
			// FIXME exposing the root classpath is a security risk
			// eg http://localhost:8080/org/springframework/zero/Banner.class
			registry.addResourceHandler("/resources/**").addResourceLocations("/")
					.addResourceLocations("classpath:/META-INF/resources/")
					.addResourceLocations("classpath:/resources/")
					.addResourceLocations("classpath:/");
			registry.addResourceHandler("/**").addResourceLocations("/")
					.addResourceLocations("classpath:/META-INF/resources/")
					.addResourceLocations("classpath:/static/")
					.addResourceLocations("classpath:/");
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
