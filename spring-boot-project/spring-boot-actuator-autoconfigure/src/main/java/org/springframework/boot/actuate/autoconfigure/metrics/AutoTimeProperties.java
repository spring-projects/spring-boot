/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

/**
 * Nested configuration properties for items that are automatically timed.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.2.0
 */
public final class AutoTimeProperties {

	/**
	 * Whether to enable auto-timing.
	 */
	private boolean enabled = true;

	/**
	 * Whether to publish percentile histograms.
	 */
	private boolean percentilesHistogram;

	/**
	 * Percentiles for which additional time series should be published.
	 */
	private double[] percentiles;

	/**
	 * Create an instance that automatically time requests with no percentiles.
	 */
	public AutoTimeProperties() {
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPercentilesHistogram() {
		return this.percentilesHistogram;
	}

	public void setPercentilesHistogram(boolean percentilesHistogram) {
		this.percentilesHistogram = percentilesHistogram;
	}

	public double[] getPercentiles() {
		return this.percentiles;
	}

	public void setPercentiles(double[] percentiles) {
		this.percentiles = percentiles;
	}

}
