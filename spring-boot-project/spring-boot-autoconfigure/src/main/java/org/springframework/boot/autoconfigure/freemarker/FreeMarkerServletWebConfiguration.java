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

package org.springframework.boot.autoconfigure.freemarker;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.servlet.ConditionalOnMissingFilterBean;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ Servlet.class, FreeMarkerConfigurer.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
class FreeMarkerServletWebConfiguration extends AbstractFreeMarkerConfiguration {

	/**
     * Constructs a new FreeMarkerServletWebConfiguration with the specified properties.
     *
     * @param properties the FreeMarker properties to be used
     */
    protected FreeMarkerServletWebConfiguration(FreeMarkerProperties properties) {
		super(properties);
	}

	/**
     * Creates and configures a FreeMarkerConfigurer bean if no other bean of type FreeMarkerConfig is present.
     * 
     * @return The configured FreeMarkerConfigurer bean.
     */
    @Bean
	@ConditionalOnMissingBean(FreeMarkerConfig.class)
	FreeMarkerConfigurer freeMarkerConfigurer() {
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		applyProperties(configurer);
		return configurer;
	}

	/**
     * Returns the FreeMarker configuration based on the provided FreeMarkerConfig object.
     * 
     * @param configurer the FreeMarkerConfig object used to retrieve the configuration
     * @return the FreeMarker configuration
     */
    @Bean
	freemarker.template.Configuration freeMarkerConfiguration(FreeMarkerConfig configurer) {
		return configurer.getConfiguration();
	}

	/**
     * Creates a FreeMarkerViewResolver bean if it is missing and the property "spring.freemarker.enabled" is either not present or set to true.
     * 
     * @return The FreeMarkerViewResolver bean.
     */
    @Bean
	@ConditionalOnMissingBean(name = "freeMarkerViewResolver")
	@ConditionalOnProperty(name = "spring.freemarker.enabled", matchIfMissing = true)
	FreeMarkerViewResolver freeMarkerViewResolver() {
		FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
		getProperties().applyToMvcViewResolver(resolver);
		return resolver;
	}

	/**
     * Registers a {@link ResourceUrlEncodingFilter} as a filter bean if it is not already present.
     * This filter is enabled only if the resource chain is enabled.
     * 
     * @return the {@link FilterRegistrationBean} for the {@link ResourceUrlEncodingFilter}
     */
    @Bean
	@ConditionalOnEnabledResourceChain
	@ConditionalOnMissingFilterBean(ResourceUrlEncodingFilter.class)
	FilterRegistrationBean<ResourceUrlEncodingFilter> resourceUrlEncodingFilter() {
		FilterRegistrationBean<ResourceUrlEncodingFilter> registration = new FilterRegistrationBean<>(
				new ResourceUrlEncodingFilter());
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
		return registration;
	}

}
