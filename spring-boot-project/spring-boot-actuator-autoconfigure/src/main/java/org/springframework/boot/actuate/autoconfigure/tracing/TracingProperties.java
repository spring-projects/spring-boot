/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for tracing.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@ConfigurationProperties("management.tracing")
public class TracingProperties {

	/**
	 * Sampling configuration.
	 */
	private final Sampling sampling = new Sampling();

	public Sampling getSampling() {
		return this.sampling;
	}

	public static class Sampling {

		/**
		 * Probability in the range from 0.0 to 1.0 that a trace will be sampled.
		 */
		private float probability = 0.10f;

		public float getProbability() {
			return this.probability;
		}

		public void setProbability(float probability) {
			this.probability = probability;
		}

	}

}
