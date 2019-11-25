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

package org.springframework.boot.actuate.autoconfigure.metrics.web.servlet;

import javax.servlet.DispatcherType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web.Server.ServerRequest;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.LongTaskTimingHandlerInterceptor;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring Web
 * MVC servlet-based request mappings.
 *
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class WebMvcMetricsAutoConfiguration {

	private final MetricsProperties properties;

	public WebMvcMetricsAutoConfiguration(MetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(WebMvcTagsProvider.class)
	public DefaultWebMvcTagsProvider webMvcTagsProvider() {
		return new DefaultWebMvcTagsProvider();
	}

	@Bean
	public FilterRegistrationBean<WebMvcMetricsFilter> webMvcMetricsFilter(MeterRegistry registry,
			WebMvcTagsProvider tagsProvider) {
		ServerRequest request = this.properties.getWeb().getServer().getRequest();
		WebMvcMetricsFilter filter = new WebMvcMetricsFilter(registry, tagsProvider, request.getMetricName(),
				request.getAutotime());
		FilterRegistrationBean<WebMvcMetricsFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	@Bean
	@Order(0)
	public MeterFilter metricsHttpServerUriTagFilter() {
		String metricName = this.properties.getWeb().getServer().getRequest().getMetricName();
		MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
				() -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
		return MeterFilter.maximumAllowableTags(metricName, "uri", this.properties.getWeb().getServer().getMaxUriTags(),
				filter);
	}

	@Bean
	public MetricsWebMvcConfigurer metricsWebMvcConfigurer(MeterRegistry meterRegistry,
			WebMvcTagsProvider tagsProvider) {
		return new MetricsWebMvcConfigurer(meterRegistry, tagsProvider);
	}

	/**
	 * {@link WebMvcConfigurer} to add metrics interceptors.
	 */
	static class MetricsWebMvcConfigurer implements WebMvcConfigurer {

		private final MeterRegistry meterRegistry;

		private final WebMvcTagsProvider tagsProvider;

		MetricsWebMvcConfigurer(MeterRegistry meterRegistry, WebMvcTagsProvider tagsProvider) {
			this.meterRegistry = meterRegistry;
			this.tagsProvider = tagsProvider;
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(new LongTaskTimingHandlerInterceptor(this.meterRegistry, this.tagsProvider));
		}

	}

}
