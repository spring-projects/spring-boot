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

package org.springframework.boot.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.mvc.FrameworkEndpoint;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link Endpoint} to expose {@link PublicMetrics}.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "endpoints.metrics", ignoreUnknownFields = false)
@FrameworkEndpoint
public class MetricsEndpoint extends AbstractEndpoint<Map<String, Object>> {

	private PublicMetrics metrics;

	/**
	 * Create a new {@link MetricsEndpoint} instance.
	 * 
	 * @param metrics the metrics to expose
	 */
	public MetricsEndpoint(PublicMetrics metrics) {
		super("/metrics");
		Assert.notNull(metrics, "Metrics must not be null");
		this.metrics = metrics;
	}

	@Override
	@RequestMapping
	@ResponseBody
	public Map<String, Object> invoke() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Metric metric : this.metrics.metrics()) {
			result.put(metric.getName(), metric.getValue());
		}
		return result;
	}

	@RequestMapping("/{name:.*}")
	@ResponseBody
	public Object value(@PathVariable String name) {
		Object value = invoke().get(name);
		if (value == null) {
			throw new NoSuchMetricException("No such metric: " + name);
		}
		return value;
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such metric")
	public static class NoSuchMetricException extends RuntimeException {

		public NoSuchMetricException(String string) {
			super(string);
		}

	}
}
