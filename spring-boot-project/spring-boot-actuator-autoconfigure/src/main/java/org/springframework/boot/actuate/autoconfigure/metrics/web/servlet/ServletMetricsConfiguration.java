/*
 * Copyright 2012-2018 the original author or authors.
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

import javax.servlet.DispatcherType;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultServletTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.MetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.ServletTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

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
public class ServletMetricsConfiguration {

	@Bean
	@ConditionalOnMissingBean(ServletTagsProvider.class)
	public DefaultServletTagsProvider servletTagsProvider() {
		return new DefaultServletTagsProvider();
	}

	@Bean
	public FilterRegistrationBean<MetricsFilter> webMetricsFilter(MeterRegistry registry, MetricsProperties properties,
			ServletTagsProvider tagsProvider,
			WebApplicationContext ctx) {
		FilterRegistrationBean<MetricsFilter> registrationBean = new FilterRegistrationBean<>(
				new MetricsFilter(registry, tagsProvider,
						properties.getWeb().getServer().getRequestsMetricName(),
						properties.getWeb().getServer().isAutoTimeRequests(),
						ctx));
		registrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registrationBean;
	}
}
