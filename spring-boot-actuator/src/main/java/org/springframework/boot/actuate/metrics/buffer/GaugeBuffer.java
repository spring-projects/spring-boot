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

package org.springframework.boot.actuate.metrics.buffer;

/**
 * Mutable buffer containing a double value and a timestamp.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class GaugeBuffer extends Buffer<Double> {

	private volatile double value;

	public GaugeBuffer(long timestamp) {
		super(timestamp);
		this.value = 0;
	}

	@Override
	public Double getValue() {
		return this.value;
	}

	public void setValue(double value) {
		this.value = value;
	}

}
