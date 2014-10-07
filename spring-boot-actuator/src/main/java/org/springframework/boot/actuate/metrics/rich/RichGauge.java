/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.metrics.rich;

import org.springframework.util.Assert;

/**
 * A gauge which stores the maximum, minimum and average in addition to the current value.
 * <p>
 * The value of the average will depend on whether a weight ('alpha') is set for the
 * gauge. If it is unset, the average will contain a simple arithmetic mean. If a weight
 * is set, an exponential moving average will be calculated as defined in this <a
 * href="http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc431.htm">NIST
 * document</a>.
 *
 * @author Luke Taylor
 */
public final class RichGauge {

	public static final String COUNT = ".count";

	public static final String MAX = ".max";

	public static final String MIN = ".min";

	public static final String AVG = ".avg";

	public static final String ALPHA = ".alpha";

	public static final String VAL = ".val";

	private final String name;

	private double value;

	private double average;

	private double max;

	private double min;

	private long count;

	private double alpha;

	/**
	 * Creates an "empty" gauge. The average, max and min will be zero, but this initial
	 * value will not be included after the first value has been set on the gauge.
	 * @param name the name under which the gauge will be stored.
	 */
	public RichGauge(String name) {
		this(name, 0.0);
		this.count = 0;
	}

	public RichGauge(String name, double value) {
		Assert.notNull(name, "The gauge name cannot be null or empty");
		this.name = name;
		this.value = value;
		this.average = this.value;
		this.min = this.value;
		this.max = this.value;
		this.alpha = -1.0;
		this.count = 1;
	}

	public RichGauge(String name, double value, double alpha, double mean, double max,
			double min, long count) {
		this.name = name;
		this.value = value;
		this.alpha = alpha;
		this.average = mean;
		this.max = max;
		this.min = min;
		this.count = count;
	}

	/**
	 * @return the name of the gauge
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the current value
	 */
	public double getValue() {
		return this.value;
	}

	/**
	 * Either an exponential weighted moving average or a simple mean, respectively,
	 * depending on whether the weight 'alpha' has been set for this gauge.
	 *
	 * @return The average over all the accumulated values
	 */
	public double getAverage() {
		return this.average;
	}

	/**
	 * @return the maximum value
	 */
	public double getMax() {
		return this.max;
	}

	/**
	 * @return the minimum value
	 */
	public double getMin() {
		return this.min;
	}

	/**
	 * @return Number of times the value has been set.
	 */
	public long getCount() {
		return this.count;
	}

	/**
	 * @return the smoothing constant value.
	 */
	public double getAlpha() {
		return this.alpha;
	}

	public RichGauge setAlpha(double alpha) {
		Assert.isTrue(alpha == -1 || (alpha > 0.0 && alpha < 1.0),
				"Smoothing constant must be between 0 and 1, or -1 to use arithmetic mean");
		this.alpha = alpha;
		return this;
	}

	RichGauge set(double value) {
		if (this.count == 0) {
			this.max = value;
			this.min = value;
		}
		else if (value > this.max) {
			this.max = value;
		}
		else if (value < this.min) {
			this.min = value;
		}

		if (this.alpha > 0.0 && this.count > 0) {
			this.average = this.alpha * this.value + (1 - this.alpha) * this.average;
		}
		else {
			double sum = this.average * this.count;
			sum += value;
			this.average = sum / (this.count + 1);
		}
		this.count++;
		this.value = value;
		return this;
	}

	RichGauge reset() {
		this.value = 0.0;
		this.max = 0.0;
		this.min = 0.0;
		this.average = 0.0;
		this.count = 0;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		RichGauge richGauge = (RichGauge) o;

		if (!this.name.equals(richGauge.name)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String toString() {
		return "Gauge [name = " + this.name + ", value = " + this.value + ", alpha = "
				+ this.alpha + ", average = " + this.average + ", max = " + this.max
				+ ", min = " + this.min + ", count = " + this.count + "]";
	}

}
