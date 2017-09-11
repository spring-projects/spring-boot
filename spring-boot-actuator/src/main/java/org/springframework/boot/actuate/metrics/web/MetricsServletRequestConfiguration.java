/**
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.boot.actuate.metrics.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Configures instrumentation of Spring Web MVC servlet-based request mappings.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(MetricsConfigurationProperties.class)
public class MetricsServletRequestConfiguration implements WebMvcConfigurer {
    @Bean
    @ConditionalOnMissingBean(WebServletTagConfigurer.class)
    public WebServletTagConfigurer webmvcTagConfigurer() {
        return new WebServletTagConfigurer();
    }

    @Bean
	public ControllerMetrics controllerMetrics(MeterRegistry registry,
										MetricsConfigurationProperties properties,
										WebServletTagConfigurer configurer) {
        return new ControllerMetrics(registry, properties, configurer);
    }

    @Bean
	public MetricsHandlerInterceptor webMetricsInterceptor(ControllerMetrics controllerMetrics) {
        return new MetricsHandlerInterceptor(controllerMetrics);
    }

    @Configuration
	public class MetricsServletRequestInterceptorConfiguration implements WebMvcConfigurer {
        @Autowired
		MetricsHandlerInterceptor handlerInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(handlerInterceptor);
        }
    }
}