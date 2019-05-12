/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.metrics;

import java.util.List;

/**
 * Settings for requests that are automatically timed.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public final class Autotime {

	private boolean enabled = true;

	private boolean percentilesHistogram;

	private double[] percentiles;

	/**
	 * Create an instance that automatically time requests with no percentiles.
	 */
	public Autotime() {
	}

	/**
	 * Create an instance with the specified settings.
	 * @param enabled whether requests should be automatically timed
	 * @param percentilesHistogram whether percentile histograms should be published
	 * @param percentiles computed non-aggregable percentiles to publish (can be
	 * {@code null})
	 */
	public Autotime(boolean enabled, boolean percentilesHistogram,
			List<Double> percentiles) {
		this.enabled = enabled;
		this.percentilesHistogram = percentilesHistogram;
		this.percentiles = (percentiles != null)
				? percentiles.stream().mapToDouble(Double::doubleValue).toArray() : null;
	}

	/**
	 * Create an instance that disable auto-timed requests.
	 * @return an instance that disable auto-timed requests
	 */
	public static Autotime disabled() {
		return new Autotime(false, false, null);
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
