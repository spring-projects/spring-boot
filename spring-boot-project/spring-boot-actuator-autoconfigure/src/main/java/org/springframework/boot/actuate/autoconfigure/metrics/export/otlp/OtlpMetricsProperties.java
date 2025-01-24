/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.otlp;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring OTLP metrics
 * export.
 *
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
@ConfigurationProperties("management.otlp.metrics.export")
public class OtlpMetricsProperties extends StepRegistryProperties {

	/**
	 * URI of the OTLP server.
	 */
	private String url = "http://localhost:4318/v1/metrics";

	/**
	 * Aggregation temporality of sums. It defines the way additive values are expressed.
	 * This setting depends on the backend you use, some only support one temporality.
	 */
	private AggregationTemporality aggregationTemporality = AggregationTemporality.CUMULATIVE;

	/**
	 * Monitored resource's attributes.
	 */
	private Map<String, String> resourceAttributes;

	/**
	 * Headers for the exported metrics.
	 */
	private Map<String, String> headers;

	/**
	 * Histogram type to be preferred when histogram publishing is enabled.
	 */
	private HistogramFlavor histogramFlavor = HistogramFlavor.EXPLICIT_BUCKET_HISTOGRAM;

	/**
	 * Max scale to use for exponential histograms, if configured.
	 */
	private int maxScale = 20;

	/**
	 * Maximum number of buckets to be used for exponential histograms, if configured.
	 * This has no effect on explicit bucket histograms.
	 */
	private int maxBucketCount = 160;

	/**
	 * Time unit for exported metrics.
	 */
	private TimeUnit baseTimeUnit = TimeUnit.MILLISECONDS;

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public AggregationTemporality getAggregationTemporality() {
		return this.aggregationTemporality;
	}

	public void setAggregationTemporality(AggregationTemporality aggregationTemporality) {
		this.aggregationTemporality = aggregationTemporality;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	@DeprecatedConfigurationProperty(replacement = "management.opentelemetry.resource-attributes", since = "3.2.0")
	public Map<String, String> getResourceAttributes() {
		return this.resourceAttributes;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setResourceAttributes(Map<String, String> resourceAttributes) {
		this.resourceAttributes = resourceAttributes;
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public HistogramFlavor getHistogramFlavor() {
		return this.histogramFlavor;
	}

	public void setHistogramFlavor(HistogramFlavor histogramFlavor) {
		this.histogramFlavor = histogramFlavor;
	}

	public int getMaxScale() {
		return this.maxScale;
	}

	public void setMaxScale(int maxScale) {
		this.maxScale = maxScale;
	}

	public int getMaxBucketCount() {
		return this.maxBucketCount;
	}

	public void setMaxBucketCount(int maxBucketCount) {
		this.maxBucketCount = maxBucketCount;
	}

	public TimeUnit getBaseTimeUnit() {
		return this.baseTimeUnit;
	}

	public void setBaseTimeUnit(TimeUnit baseTimeUnit) {
		this.baseTimeUnit = baseTimeUnit;
	}

}
