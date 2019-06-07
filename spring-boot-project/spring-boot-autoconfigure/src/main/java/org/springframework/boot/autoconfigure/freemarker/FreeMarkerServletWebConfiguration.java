/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * Configuration for FreeMarker when used in a servlet web context.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ Servlet.class, FreeMarkerConfigurer.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
class FreeMarkerServletWebConfiguration extends AbstractFreeMarkerConfiguration {

	protected FreeMarkerServletWebConfiguration(FreeMarkerProperties properties) {
		super(properties);
	}

	@Bean
	@ConditionalOnMissingBean(FreeMarkerConfig.class)
	public FreeMarkerConfigurer freeMarkerConfigurer() {
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		applyProperties(configurer);
		return configurer;
	}

	@Bean
	public freemarker.template.Configuration freeMarkerConfiguration(FreeMarkerConfig configurer) {
		return configurer.getConfiguration();
	}

	@Bean
	@ConditionalOnMissingBean(name = "freeMarkerViewResolver")
	@ConditionalOnProperty(name = "spring.freemarker.enabled", matchIfMissing = true)
	public FreeMarkerViewResolver freeMarkerViewResolver() {
		FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
		getProperties().applyToMvcViewResolver(resolver);
		return resolver;
	}

	@Bean
	@ConditionalOnMissingBean(ResourceUrlEncodingFilter.class)
	@ConditionalOnEnabledResourceChain
	public FilterRegistrationBean<ResourceUrlEncodingFilter> resourceUrlEncodingFilter() {
		FilterRegistrationBean<ResourceUrlEncodingFilter> registration = new FilterRegistrationBean<>(
				new ResourceUrlEncodingFilter());
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
		return registration;
	}

}
