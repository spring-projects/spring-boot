/*
 * Copyright 2012-2024 the original author or authors.
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
 * @author Jonatan Ivanov
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

	/**
	 * Brave configuration.
	 */
	private final Brave brave = new Brave();

	/**
     * Returns the sampling object associated with this TracingProperties instance.
     *
     * @return the sampling object
     */
    public Sampling getSampling() {
		return this.sampling;
	}

	/**
     * Returns the baggage associated with this TracingProperties object.
     *
     * @return the baggage associated with this TracingProperties object
     */
    public Baggage getBaggage() {
		return this.baggage;
	}

	/**
     * Returns the propagation object associated with this TracingProperties instance.
     *
     * @return the propagation object
     */
    public Propagation getPropagation() {
		return this.propagation;
	}

	/**
     * Returns the Brave object associated with this TracingProperties instance.
     *
     * @return the Brave object associated with this TracingProperties instance
     */
    public Brave getBrave() {
		return this.brave;
	}

	/**
     * Sampling class.
     */
    public static class Sampling {

		/**
		 * Probability in the range from 0.0 to 1.0 that a trace will be sampled.
		 */
		private float probability = 0.10f;

		/**
         * Returns the probability value of the Sampling object.
         *
         * @return the probability value of the Sampling object.
         */
        public float getProbability() {
			return this.probability;
		}

		/**
         * Sets the probability value for the sampling.
         * 
         * @param probability the probability value to be set
         */
        public void setProbability(float probability) {
			this.probability = probability;
		}

	}

	/**
     * Baggage class.
     */
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

		/**
		 * List of fields that should be accessible within the JVM process but not
		 * propagated over the wire. Local fields are not supported with OpenTelemetry.
		 */
		private List<String> localFields = new ArrayList<>();

		/**
		 * List of fields that should automatically become tags.
		 */
		private List<String> tagFields = new ArrayList<>();

		/**
         * Returns the current status of the baggage.
         * 
         * @return true if the baggage is enabled, false otherwise
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Baggage.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the correlation of the baggage.
         *
         * @return the correlation of the baggage
         */
        public Correlation getCorrelation() {
			return this.correlation;
		}

		/**
         * Sets the correlation for the baggage.
         * 
         * @param correlation the correlation to be set
         */
        public void setCorrelation(Correlation correlation) {
			this.correlation = correlation;
		}

		/**
         * Returns the list of remote fields.
         * 
         * @return the list of remote fields
         */
        public List<String> getRemoteFields() {
			return this.remoteFields;
		}

		/**
         * Returns the list of local fields in the Baggage class.
         *
         * @return the list of local fields
         */
        public List<String> getLocalFields() {
			return this.localFields;
		}

		/**
         * Returns the list of tag fields.
         * 
         * @return the list of tag fields
         */
        public List<String> getTagFields() {
			return this.tagFields;
		}

		/**
         * Sets the list of remote fields for the Baggage.
         * 
         * @param remoteFields the list of remote fields to be set
         */
        public void setRemoteFields(List<String> remoteFields) {
			this.remoteFields = remoteFields;
		}

		/**
         * Sets the local fields of the Baggage class.
         * 
         * @param localFields the list of local fields to be set
         */
        public void setLocalFields(List<String> localFields) {
			this.localFields = localFields;
		}

		/**
         * Sets the tag fields for the baggage.
         * 
         * @param tagFields the list of tag fields to be set
         */
        public void setTagFields(List<String> tagFields) {
			this.tagFields = tagFields;
		}

		/**
         * Correlation class.
         */
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

			/**
             * Returns the current state of the enabled flag.
             *
             * @return true if the flag is enabled, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Correlation.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			/**
             * Returns the list of fields in the Correlation class.
             *
             * @return the list of fields
             */
            public List<String> getFields() {
				return this.fields;
			}

			/**
             * Sets the fields of the Correlation object.
             * 
             * @param fields the list of fields to be set
             */
            public void setFields(List<String> fields) {
				this.fields = fields;
			}

		}

	}

	/**
     * Propagation class.
     */
    public static class Propagation {

		/**
		 * Tracing context propagation types produced and consumed by the application.
		 * Setting this property overrides the more fine-grained propagation type
		 * properties.
		 */
		private List<PropagationType> type;

		/**
		 * Tracing context propagation types produced by the application.
		 */
		private List<PropagationType> produce = List.of(PropagationType.W3C);

		/**
		 * Tracing context propagation types consumed by the application.
		 */
		private List<PropagationType> consume = List.of(PropagationType.values());

		/**
         * Sets the type of propagation for this object.
         * 
         * @param type the list of propagation types to be set
         */
        public void setType(List<PropagationType> type) {
			this.type = type;
		}

		/**
         * Sets the list of propagation types for produce.
         * 
         * @param produce the list of propagation types for produce
         */
        public void setProduce(List<PropagationType> produce) {
			this.produce = produce;
		}

		/**
         * Sets the list of propagation types to consume.
         * 
         * @param consume the list of propagation types to consume
         */
        public void setConsume(List<PropagationType> consume) {
			this.consume = consume;
		}

		/**
         * Returns the list of propagation types.
         * 
         * @return the list of propagation types
         */
        public List<PropagationType> getType() {
			return this.type;
		}

		/**
         * Returns the list of propagation types for produce.
         *
         * @return the list of propagation types for produce
         */
        public List<PropagationType> getProduce() {
			return this.produce;
		}

		/**
         * Returns the list of PropagationType objects representing the consume types.
         *
         * @return the list of PropagationType objects representing the consume types
         */
        public List<PropagationType> getConsume() {
			return this.consume;
		}

		/**
		 * Returns the effective context propagation types produced by the application.
		 * This will be {@link #getType()} if set or {@link #getProduce()} otherwise.
		 * @return the effective context propagation types produced by the application
		 */
		List<PropagationType> getEffectiveProducedTypes() {
			return (this.type != null) ? this.type : this.produce;
		}

		/**
		 * Returns the effective context propagation types consumed by the application.
		 * This will be {@link #getType()} if set or {@link #getConsume()} otherwise.
		 * @return the effective context propagation types consumed by the application
		 */
		List<PropagationType> getEffectiveConsumedTypes() {
			return (this.type != null) ? this.type : this.consume;
		}

		/**
		 * Supported propagation types. The declared order of the values matter.
		 */
		public enum PropagationType {

			/**
			 * <a href="https://www.w3.org/TR/trace-context/">W3C</a> propagation.
			 */
			W3C,

			/**
			 * <a href="https://github.com/openzipkin/b3-propagation#single-header">B3
			 * single header</a> propagation.
			 */
			B3,

			/**
			 * <a href="https://github.com/openzipkin/b3-propagation#multiple-headers">B3
			 * multiple headers</a> propagation.
			 */
			B3_MULTI

		}

	}

	/**
     * Brave class.
     */
    public static class Brave {

		/**
		 * Whether the propagation type and tracing backend support sharing the span ID
		 * between client and server spans. Requires B3 propagation and a compatible
		 * backend.
		 */
		private boolean spanJoiningSupported = false;

		/**
         * Returns a boolean value indicating whether span joining is supported.
         * 
         * @return true if span joining is supported, false otherwise
         */
        public boolean isSpanJoiningSupported() {
			return this.spanJoiningSupported;
		}

		/**
         * Sets whether span joining is supported.
         * 
         * @param spanJoiningSupported true if span joining is supported, false otherwise
         */
        public void setSpanJoiningSupported(boolean spanJoiningSupported) {
			this.spanJoiningSupported = spanJoiningSupported;
		}

	}

}
