/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring metrics export to Prometheus.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.prometheus")
public class PrometheusProperties {

	/**
	 * Enable publishing to Prometheus.
	 */
	private Boolean enabled = true;

	/**
	 * Enable publishing descriptions as part of the scrape payload to Prometheus. Turn
	 * this off to minimize the amount of data sent on each scrape.
	 */
	private Boolean descriptions = true;

	/**
	 * The bucket filter clamping the bucket domain of timer percentiles histograms to
	 * some max value. This is used to limit the number of buckets shipped to Prometheus
	 * to save on storage.
	 */
	private Duration timerPercentilesMax = Duration.ofMinutes(2);

	/**
	 * The bucket filter clamping the bucket domain of timer percentiles histograms to
	 * some min value. This is used to limit the number of buckets shipped to Prometheus
	 * to save on storage.
	 */
	private Duration timerPercentilesMin = Duration.ofMillis(10);

	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getDescriptions() {
		return this.descriptions;
	}

	public void setDescriptions(Boolean descriptions) {
		this.descriptions = descriptions;
	}

	public Duration getTimerPercentilesMax() {
		return this.timerPercentilesMax;
	}

	public void setTimerPercentilesMax(Duration timerPercentilesMax) {
		this.timerPercentilesMax = timerPercentilesMax;
	}

	public Duration getTimerPercentilesMin() {
		return this.timerPercentilesMin;
	}

	public void setTimerPercentilesMin(Duration timerPercentilesMin) {
		this.timerPercentilesMin = timerPercentilesMin;
	}

}
