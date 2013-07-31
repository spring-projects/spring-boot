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

package org.springframework.boot.ops.autoconfigure;

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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.ops.metrics.CounterService;
import org.springframework.boot.ops.metrics.GaugeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that records Servlet interactions
 * with a {@link CounterService} and {@link GaugeService}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnBean({ CounterService.class, GaugeService.class })
@ConditionalOnClass({ Servlet.class })
@AutoConfigureAfter(MetricRepositoryAutoConfiguration.class)
public class MetricFilterAutoConfiguration {

	private static final int UNDEFINED_HTTP_STATUS = 999;

	@Autowired
	private CounterService counterService;

	@Autowired
	private GaugeService gaugeService;

	@Bean
	public Filter metricFilter() {
		return new MetricsFilter();
	}

	/**
	 * Filter that counts requests and measures processing times.
	 */
	@Order(Ordered.HIGHEST_PRECEDENCE)
	private final class MetricsFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			if ((request instanceof HttpServletRequest)
					&& (response instanceof HttpServletResponse)) {
				doFilter((HttpServletRequest) request, (HttpServletResponse) response,
						chain);
			}
			else {
				chain.doFilter(request, response);
			}
		}

		public void doFilter(HttpServletRequest request, HttpServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			UrlPathHelper helper = new UrlPathHelper();
			String suffix = helper.getPathWithinApplication(request);
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			try {
				chain.doFilter(request, response);
			}
			finally {
				stopWatch.stop();
				String gaugeKey = getKey("response" + suffix);
				MetricFilterAutoConfiguration.this.gaugeService.set(gaugeKey,
						stopWatch.getTotalTimeMillis());
				String counterKey = getKey("status." + getStatus(response) + suffix);
				MetricFilterAutoConfiguration.this.counterService.increment(counterKey);
			}
		}

		private int getStatus(HttpServletResponse response) {
			try {
				return response.getStatus();
			}
			catch (Exception ex) {
				return UNDEFINED_HTTP_STATUS;
			}
		}

		private String getKey(String string) {
			// graphite compatible metric names
			String value = string.replace("/", ".");
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
