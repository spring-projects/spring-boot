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

import java.util.ArrayList;
import java.util.List;

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
	 * Whether auto-configuration of tracing is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Sampling configuration.
	 */
	private final Sampling sampling = new Sampling();

	/**
	 * Baggage configuration.
	 */
	private final Baggage baggage = new Baggage();

	/**
	 * Propagation configuration.
	 */
	private final Propagation propagation = new Propagation();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Sampling getSampling() {
		return this.sampling;
	}

	public Baggage getBaggage() {
		return this.baggage;
	}

	public Propagation getPropagation() {
		return this.propagation;
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

	public static class Baggage {

		/**
		 * Whether to enable Micrometer Tracing baggage propagation.
		 */
		private boolean enabled = true;

		/**
		 * Correlation configuration.
		 */
		private Correlation correlation = new Correlation();

		/**
		 * List of fields that are referenced the same in-process as it is on the wire.
		 * For example, the field "x-vcap-request-id" would be set as-is including the
		 * prefix.
		 */
		private List<String> remoteFields = new ArrayList<>();

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Correlation getCorrelation() {
			return this.correlation;
		}

		public void setCorrelation(Correlation correlation) {
			this.correlation = correlation;
		}

		public List<String> getRemoteFields() {
			return this.remoteFields;
		}

		public void setRemoteFields(List<String> remoteFields) {
			this.remoteFields = remoteFields;
		}

		public static class Correlation {

			/**
			 * Whether to enable correlation of the baggage context with logging contexts.
			 */
			private boolean enabled = true;

			/**
			 * List of fields that should be correlated with the logging context. That
			 * means that these fields would end up as key-value pairs in e.g. MDC.
			 */
			private List<String> fields = new ArrayList<>();

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public List<String> getFields() {
				return this.fields;
			}

			public void setFields(List<String> fields) {
				this.fields = fields;
			}

		}

	}

	public static class Propagation {

		/**
		 * Tracing context propagation type.
		 */
		private PropagationType type = PropagationType.W3C;

		public PropagationType getType() {
			return this.type;
		}

		public void setType(PropagationType type) {
			this.type = type;
		}

		enum PropagationType {

			/**
			 * B3 propagation type.
			 */
			B3,

			/**
			 * W3C propagation type.
			 */
			W3C

		}

	}

}
