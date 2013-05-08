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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.metrics.CounterService;
import org.springframework.bootstrap.actuate.metrics.GaugeService;
import org.springframework.bootstrap.context.annotation.ConditionalOnBean;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for service apps.
 * 
 * @author Dave Syer
 */
@Configuration
// FIXME: make this conditional
// @ConditionalOnBean({ CounterService.class, GaugeService.class })
@ConditionalOnClass({ Servlet.class })
public class MetricFilterConfiguration {

	@Autowired(required = false)
	private CounterService counterService;

	@Autowired(required = false)
	private GaugeService gaugeService;

	@Bean
	@ConditionalOnBean({ CounterService.class, GaugeService.class })
	public Filter metricFilter() {
		return new CounterServiceFilter();
	}

	/**
	 * Filter that counts requests and measures processing times.
	 * 
	 * @author Dave Syer
	 * 
	 */
	@Order(Integer.MIN_VALUE)
	// TODO: parameterize the order (ideally it runs before any other filter)
	private final class CounterServiceFilter extends GenericFilterBean {
		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			HttpServletRequest servletRequest = (HttpServletRequest) request;
			HttpServletResponse servletResponse = (HttpServletResponse) response;
			UrlPathHelper helper = new UrlPathHelper();
			String suffix = helper.getPathWithinApplication(servletRequest);
			int status = 999;
			long t0 = System.currentTimeMillis();
			try {
				chain.doFilter(request, response);
			} finally {
				try {
					status = servletResponse.getStatus();
				} catch (Exception e) {
					// ignore
				}
				set("response", suffix, System.currentTimeMillis() - t0);
				increment("status." + status, suffix);
			}
		}

		private void increment(String prefix, String suffix) {
			if (MetricFilterConfiguration.this.counterService != null) {
				String key = getKey(prefix + suffix);
				MetricFilterConfiguration.this.counterService.increment(key);
			}
		}

		private void set(String prefix, String suffix, double value) {
			if (MetricFilterConfiguration.this.gaugeService != null) {
				String key = getKey(prefix + suffix);
				MetricFilterConfiguration.this.gaugeService.set(key, value);
			}
		}

		private String getKey(String string) {
			String value = string.replace("/", "."); // graphite compatible metric names
			value = value.replace("..", ".");
			if (value.endsWith(".")) {
				value = value + "root";
			}
			if (value.startsWith("_")) {
				value = value.substring(1);
			}
			return value;
		}
	}

}
