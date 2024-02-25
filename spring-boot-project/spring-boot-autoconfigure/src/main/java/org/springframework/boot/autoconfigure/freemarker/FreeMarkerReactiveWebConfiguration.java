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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfig;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;

/**
 * Configuration for FreeMarker when used in a reactive web context.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@AutoConfigureAfter(WebFluxAutoConfiguration.class)
class FreeMarkerReactiveWebConfiguration extends AbstractFreeMarkerConfiguration {

	/**
	 * Constructs a new FreeMarkerReactiveWebConfiguration with the specified properties.
	 * @param properties the properties to be used for configuring FreeMarker.
	 */
	FreeMarkerReactiveWebConfiguration(FreeMarkerProperties properties) {
		super(properties);
	}

	/**
	 * Creates and configures a FreeMarkerConfigurer bean if no bean of type
	 * FreeMarkerConfig is present.
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
	 * Returns the FreeMarker configuration based on the provided FreeMarkerConfig.
	 * @param configurer the FreeMarkerConfig used to configure the FreeMarker template
	 * engine
	 * @return the FreeMarker configuration
	 */
	@Bean
	freemarker.template.Configuration freeMarkerConfiguration(FreeMarkerConfig configurer) {
		return configurer.getConfiguration();
	}

	/**
	 * Creates a FreeMarkerViewResolver bean if it is missing and the property
	 * "spring.freemarker.enabled" is either not present or set to true.
	 * @return the FreeMarkerViewResolver bean
	 */
	@Bean
	@ConditionalOnMissingBean(name = "freeMarkerViewResolver")
	@ConditionalOnProperty(name = "spring.freemarker.enabled", matchIfMissing = true)
	FreeMarkerViewResolver freeMarkerViewResolver() {
		FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
		resolver.setPrefix(getProperties().getPrefix());
		resolver.setSuffix(getProperties().getSuffix());
		resolver.setRequestContextAttribute(getProperties().getRequestContextAttribute());
		resolver.setViewNames(getProperties().getViewNames());
		return resolver;
	}

}
