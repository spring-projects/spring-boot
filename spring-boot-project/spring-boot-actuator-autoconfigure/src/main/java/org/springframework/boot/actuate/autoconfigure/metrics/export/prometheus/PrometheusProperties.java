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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring metrics export
 * to Prometheus.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.prometheus.metrics.export")
public class PrometheusProperties {

	/**
	 * Whether exporting of metrics to this backend is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Whether to enable publishing descriptions as part of the scrape payload to
	 * Prometheus. Turn this off to minimize the amount of data sent on each scrape.
	 */
	private boolean descriptions = true;

	/**
	 * Additional properties to pass to the Prometheus client.
	 */
	private final Map<String, String> properties = new HashMap<>();

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	public boolean isDescriptions() {
		return this.descriptions;
	}

	public void setDescriptions(boolean descriptions) {
		this.descriptions = descriptions;
	}

	public Duration getStep() {
		return this.step;
	}

	public void setStep(Duration step) {
		this.step = step;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

}
