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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose a collection of {@link PublicMetrics}.
 *
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "endpoints.metrics", ignoreUnknownFields = false)
public class MetricsEndpoint extends AbstractEndpoint<Map<String, Object>> {

	private final List<PublicMetrics> publicMetrics;

	/**
	 * Create a new {@link MetricsEndpoint} instance.
	 * @param publicMetrics the metrics to expose
	 */
	public MetricsEndpoint(PublicMetrics publicMetrics) {
		this(Collections.singleton(publicMetrics));
	}

	/**
	 * Create a new {@link MetricsEndpoint} instance.
	 * @param publicMetrics the metrics to expose. The collection will be sorted using the
	 * {@link AnnotationAwareOrderComparator}.
	 */
	public MetricsEndpoint(Collection<PublicMetrics> publicMetrics) {
		super("metrics");
		Assert.notNull(publicMetrics, "PublicMetrics must not be null");
		this.publicMetrics = new ArrayList<PublicMetrics>(publicMetrics);
		AnnotationAwareOrderComparator.sort(this.publicMetrics);
	}

	public void registerPublicMetrics(PublicMetrics metrics) {
		this.publicMetrics.add(metrics);
		AnnotationAwareOrderComparator.sort(this.publicMetrics);
	}

	public void unregisterPublicMetrics(PublicMetrics metrics) {
		this.publicMetrics.remove(metrics);
	}

	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		List<PublicMetrics> metrics = new ArrayList<PublicMetrics>(this.publicMetrics);
		for (PublicMetrics publicMetric : metrics) {
			try {
				for (Metric<?> metric : publicMetric.metrics()) {
					result.put(metric.getName(), metric.getValue());
				}
			}
			catch (Exception ex) {
				// Could not evaluate metrics
			}
		}
		return result;
	}

}
