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

package org.springframework.boot.ops.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.ops.metrics.Metric;
import org.springframework.boot.strap.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose {@link PublicMetrics}.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "endpoints.metrics", ignoreUnknownFields = false)
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
	public Map<String, Object> invoke() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Metric metric : this.metrics.metrics()) {
			result.put(metric.getName(), metric.getValue());
		}
		return result;
	}

}
