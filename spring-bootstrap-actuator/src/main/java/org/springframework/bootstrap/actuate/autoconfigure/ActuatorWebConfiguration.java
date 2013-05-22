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
package org.springframework.bootstrap.actuate.autoconfigure;

import java.util.List;

import javax.servlet.Servlet;

import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Dave Syer
 * 
 */
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@Configuration
public class ActuatorWebConfiguration extends DelegatingWebMvcConfiguration {

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		addDefaultHttpMessageConverters(converters);
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper().disable(
						SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			}
		}
		super.configureMessageConverters(converters);
	}

}
