/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.properties;

import java.time.Duration;

/**
 * Base class for properties that configure a metrics registry that pushes aggregated
 * metrics on a regular interval.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public abstract class PushRegistryProperties {

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
	 * Whether exporting of metrics to this backend is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Connection timeout for requests to this backend.
	 */
	private Duration connectTimeout = Duration.ofSeconds(1);

	/**
	 * Read timeout for requests to this backend.
	 */
	private Duration readTimeout = Duration.ofSeconds(10);

	/**
	 * Number of measurements per request to use for this backend. If more measurements
	 * are found, then multiple requests will be made.
	 */
	private Integer batchSize = 10000;

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

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Integer getBatchSize() {
		return this.batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

}
