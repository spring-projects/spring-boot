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

package org.springframework.bootstrap.autoconfigure.web;

import javax.servlet.Servlet;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.autoconfigure.web.WebMvcAutoConfiguration.WebMvcConfiguration;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebMvc Web MVC}.
 * 
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnMissingBean({ HandlerAdapter.class, HandlerMapping.class })
@Import(WebMvcConfiguration.class)
public class WebMvcAutoConfiguration {

	/**
	 * Nested configuration used because {@code @EnableWebMvc} will add HandlerAdapter and
	 * HandlerMapping, causing the condition to fail and the additional DispatcherServlet
	 * bean never to be registered if it were declared directly.
	 */
	@EnableWebMvc
	public static class WebMvcConfiguration extends WebMvcConfigurerAdapter {

		@Autowired
		private ListableBeanFactory beanFactory;

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Override
		public void configureDefaultServletHandling(
				DefaultServletHandlerConfigurer configurer) {
			configurer.enable();
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			for (Converter<?, ?> converter : this.beanFactory.getBeansOfType(
					Converter.class).values()) {
				registry.addConverter(converter);
			}
			for (GenericConverter converter : this.beanFactory.getBeansOfType(
					GenericConverter.class).values()) {
				registry.addConverter(converter);
			}
			for (Formatter<?> formatter : this.beanFactory
					.getBeansOfType(Formatter.class).values()) {
				registry.addFormatter(formatter);
			}
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/**").addResourceLocations("/")
					.addResourceLocations("classpath:/META-INF/resources/")
					.addResourceLocations("classpath:/static")
					.addResourceLocations("classpath:/");
		}

	}

}
