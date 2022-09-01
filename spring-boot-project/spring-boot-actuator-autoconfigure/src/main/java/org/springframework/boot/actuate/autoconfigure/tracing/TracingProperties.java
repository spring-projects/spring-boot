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

import java.util.HashSet;
import java.util.Set;

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

	/**
	 * Baggage configuration.
	 */
	private final Baggage baggage = new Baggage();

	/**
	 * Propagation configuration.
	 */
	private final Propagation propagation = new Propagation();

	public Sampling getSampling() {
		return this.sampling;
	}

	public Baggage getBaggage() {
		return this.baggage;
	}

	public Propagation getPropagation() {
		return propagation;
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
		 * Enables correlating the baggage context with logging contexts.
		 */
		private boolean correlationEnabled = true;

		/**
		 * List of fields that should be propagated over the wire.
		 */
		private Set<String> correlationFields = new HashSet<>();

		/**
		 * List of fields that should be accessible within the JVM process but not propagated
		 * over the wire.
		 */
		private Set<String> localFields = new HashSet<>();

		/**
		 * List of fields that are referenced the same in-process as it is on the wire. For
		 * example, the field "x-vcap-request-id" would be set as-is including the prefix.
		 */
		private Set<String> remoteFields = new HashSet<>();

		/**
		 * List of fields that should automatically become tags.
		 */
		private Set<String> tagFields = new HashSet<>();

		public boolean isCorrelationEnabled() {
			return correlationEnabled;
		}

		public void setCorrelationEnabled(boolean correlationEnabled) {
			this.correlationEnabled = correlationEnabled;
		}

		public Set<String> getCorrelationFields() {
			return correlationFields;
		}

		public void setCorrelationFields(Set<String> correlationFields) {
			this.correlationFields = correlationFields;
		}

		public Set<String> getLocalFields() {
			return localFields;
		}

		public void setLocalFields(Set<String> localFields) {
			this.localFields = localFields;
		}

		public Set<String> getRemoteFields() {
			return remoteFields;
		}

		public void setRemoteFields(Set<String> remoteFields) {
			this.remoteFields = remoteFields;
		}

		public Set<String> getTagFields() {
			return this.tagFields;
		}

		public void setTagFields(Set<String> tagFields) {
			this.tagFields = tagFields;
		}

	}

	public static class Propagation {

		/**
		 * Tracing context propagation types.
		 */
		private PropagationType type = PropagationType.B3;

		public PropagationType getType() {
			return this.type;
		}

		public void setType(PropagationType type) {
			this.type = type;
		}

		enum PropagationType {
			/**
			 * AWS propagation type.
			 */
			AWS,

			/**
			 * B3 propagation type.
			 */
			B3,

			/**
			 * W3C propagation type.
			 */
			W3C,

			/**
			 * Custom propagation type. If picked, requires bean registration overriding the
			 * default propagation mechanisms.
			 */
			CUSTOM
		}
	}
}
