/*
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.servlet;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetrics;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Configures instrumentation of Spring Web MVC servlet-based request mappings.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class WebMvcMetricsConfiguration {

	@Bean
	@ConditionalOnMissingBean(WebMvcTagsProvider.class)
	public DefaultWebMvcTagsProvider webmvcTagConfigurer() {
		return new DefaultWebMvcTagsProvider();
	}

	@Bean
	public WebMvcMetrics controllerMetrics(MeterRegistry registry,
			MetricsProperties properties, WebMvcTagsProvider configurer) {
		return new WebMvcMetrics(registry, configurer,
				properties.getWeb().getServer().getRequestsMetricName(),
				properties.getWeb().getServer().isAutoTimeRequests(),
				properties.getWeb().getServer().isRecordRequestPercentiles());
	}

	@Bean
	public WebMvcMetricsFilter webMetricsFilter(WebMvcMetrics controllerMetrics,
			HandlerMappingIntrospector introspector) {
		return new WebMvcMetricsFilter(controllerMetrics, introspector);
	}

}
