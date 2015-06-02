/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Filter that counts requests and measures processing times.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
final class MetricsFilter extends OncePerRequestFilter {

	private static final int UNDEFINED_HTTP_STATUS = 999;

	private static final String UNKNOWN_PATH_SUFFIX = "/unmapped";

	private static final Log logger = LogFactory.getLog(MetricsFilter.class);

	private final CounterService counterService;

	private final GaugeService gaugeService;

	public MetricsFilter(CounterService counterService, GaugeService gaugeService) {
		this.counterService = counterService;
		this.gaugeService = gaugeService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain) throws ServletException,
			IOException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		String path = new UrlPathHelper().getPathWithinApplication(request);
		int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
		try {
			chain.doFilter(request, response);
			status = getStatus(response);
		}
		finally {
			stopWatch.stop();
			recordMetrics(request, path, status, stopWatch.getTotalTimeMillis());
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

	private void recordMetrics(HttpServletRequest request, String path, int status,
			long time) {
		String suffix = getFinalStatus(request, path, status);
		submitToGauge(getKey("response" + suffix), time);
		incrementCounter(getKey("status." + status + suffix));
	}

	private String getFinalStatus(HttpServletRequest request, String path, int status) {
		Object bestMatchingPattern = request
				.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (bestMatchingPattern != null) {
			return fixSpecialCharacters(bestMatchingPattern.toString());
		}
		if (is4xxClientError(status)) {
			return UNKNOWN_PATH_SUFFIX;
		}
		return path;
	}

	private String fixSpecialCharacters(String value) {
		String result = value.replaceAll("[{}]", "-");
		result = result.replace("**", "-star-star-");
		result = result.replace("*", "-star-");
		result = result.replace("/-", "/");
		result = result.replace("-/", "/");
		if (result.endsWith("-")) {
			result = result.substring(0, result.length() - 1);
		}
		if (result.startsWith("-")) {
			result = result.substring(1);
		}
		return result;
	}

	private boolean is4xxClientError(int status) {
		try {
			return HttpStatus.valueOf(status).is4xxClientError();
		}
		catch (Exception ex) {
			return false;
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

	private void submitToGauge(String metricName, double value) {
		try {
			this.gaugeService.submit(metricName, value);
		}
		catch (Exception ex) {
			logger.warn("Unable to submit gauge metric '" + metricName + "'", ex);
		}
	}

	private void incrementCounter(String metricName) {
		try {
			this.counterService.increment(metricName);
		}
		catch (Exception ex) {
			logger.warn("Unable to submit counter metric '" + metricName + "'", ex);
		}
	}

}
