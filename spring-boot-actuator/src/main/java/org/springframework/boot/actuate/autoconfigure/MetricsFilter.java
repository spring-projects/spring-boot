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
import org.springframework.http.HttpStatus.Series;
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

	MetricsFilter(CounterService counterService, GaugeService gaugeService) {
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
		Series series = getSeries(status);
		if (Series.CLIENT_ERROR.equals(series) || Series.REDIRECTION.equals(series)) {
			return UNKNOWN_PATH_SUFFIX;
		}
		return path;
	}

	private String fixSpecialCharacters(String value) {
		final StringBuilder sb = new StringBuilder(value);
		replaceAll(sb, "{", "-");
		replaceAll(sb, "}", "-");
		replaceAll(sb, "**", "-star-star-");
		replaceAll(sb, "*", "-star-");
		replaceAll(sb, "/-", "/");
		replaceAll(sb, "-/", "/");
		if (sb.charAt(sb.length() - 1) == '-') {
			sb.setLength(sb.length() - 1);
		}
		if (sb.charAt(0) == '-') {
			sb.deleteCharAt(0);
		}
		return sb.toString();
	}

	// Method from http://stackoverflow.com/a/3472705
	private static void replaceAll(final StringBuilder builder, final String from, final String to) {
		int index = builder.indexOf(from);
		while (index != -1) {
			builder.replace(index, index + from.length(), to);
			index += to.length(); // Move to the end of the replacement
			index = builder.indexOf(from, index);
		}
	}

	private Series getSeries(int status) {
		try {
			return HttpStatus.valueOf(status).series();
		}
		catch (Exception ex) {
			return null;
		}

	}

	private String getKey(String string) {
		// graphite compatible metric names
		final StringBuilder sb = new StringBuilder(string);
		replaceAll(sb, "/", ".");
		replaceAll(sb, "..", ".");
		if (sb.charAt(sb.length() - 1) == '.') {
			sb.append("root");
		}
		if (sb.charAt(0) == '_') {
			sb.deleteCharAt(0);
		}
		return sb.toString();
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
