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

package org.springframework.boot.config.web;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.springframework.boot.config.EnableAutoConfiguration;
import org.springframework.boot.strap.context.condition.ConditionalOnBean;
import org.springframework.boot.strap.context.condition.ConditionalOnClass;
import org.springframework.boot.strap.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for multi-part uploads. Adds a
 * {@link StandardServletMultipartResolver} when a {@link MultipartConfigElement} bean is
 * defined. The {@link EmbeddedWebApplicationContext} will associated the
 * {@link MultipartConfigElement} bean to any {@link Servlet} beans.
 * 
 * @author Greg Turnquist
 */
@Configuration
@ConditionalOnClass({ Servlet.class, StandardServletMultipartResolver.class })
@ConditionalOnBean(MultipartConfigElement.class)
public class MultipartAutoConfiguration {

	@Bean
	public StandardServletMultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

}
