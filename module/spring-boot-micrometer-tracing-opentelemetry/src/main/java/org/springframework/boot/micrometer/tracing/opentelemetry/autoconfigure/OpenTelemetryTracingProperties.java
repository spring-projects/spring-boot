/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for tracing with OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry.tracing")
public class OpenTelemetryTracingProperties {

	/**
	 * Span export configuration.
	 */
	private final Export export = new Export();

	/**
	 * Span limit configuration.
	 */
	private final Limits limits = new Limits();

	/**
	 * Sampler to use.
	 */
	private Sampler sampler = Sampler.PARENT_BASED_TRACE_ID_RATIO;

	public Export getExport() {
		return this.export;
	}

	public Limits getLimits() {
		return this.limits;
	}

	public Sampler getSampler() {
		return this.sampler;
	}

	public void setSampler(Sampler sampler) {
		this.sampler = sampler;
	}

	public static class Export {

		/**
		 * Whether unsampled spans should be exported.
		 */
		private boolean includeUnsampled;

		/**
		 * Maximum time an export will be allowed to run before being cancelled.
		 */
		private Duration timeout = Duration.ofSeconds(30);

		/**
		 * Maximum batch size for each export. This must be less than or equal to
		 * 'maxQueueSize'.
		 */
		private int maxBatchSize = 512;

		/**
		 * Maximum number of spans that are kept in the queue before they will be dropped.
		 */
		private int maxQueueSize = 2048;

		/**
		 * The delay interval between two consecutive exports.
		 */
		private Duration scheduleDelay = Duration.ofSeconds(5);

		public boolean isIncludeUnsampled() {
			return this.includeUnsampled;
		}

		public void setIncludeUnsampled(boolean includeUnsampled) {
			this.includeUnsampled = includeUnsampled;
		}

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public int getMaxBatchSize() {
			return this.maxBatchSize;
		}

		public void setMaxBatchSize(int maxBatchSize) {
			this.maxBatchSize = maxBatchSize;
		}

		public int getMaxQueueSize() {
			return this.maxQueueSize;
		}

		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		public Duration getScheduleDelay() {
			return this.scheduleDelay;
		}

		public void setScheduleDelay(Duration scheduleDelay) {
			this.scheduleDelay = scheduleDelay;
		}

	}

	/**
	 * Span limits.
	 */
	public static class Limits {

		/**
		 * Maximum number of characters for string attribute values.
		 */
		private int maxAttributeValueLength = Integer.MAX_VALUE;

		/**
		 * Maximum number of attributes per span.
		 */
		private int maxAttributes = 128;

		/**
		 * Maximum number of events per span.
		 */
		private int maxEvents = 128;

		/**
		 * Maximum number of links per span.
		 */
		private int maxLinks = 128;

		/**
		 * Maximum number of attributes per event.
		 */
		private int maxAttributesPerEvent = 128;

		/**
		 * Maximum number of attributes per link.
		 */
		private int maxAttributesPerLink = 128;

		public int getMaxAttributeValueLength() {
			return this.maxAttributeValueLength;
		}

		public void setMaxAttributeValueLength(int maxAttributeValueLength) {
			this.maxAttributeValueLength = maxAttributeValueLength;
		}

		public int getMaxAttributes() {
			return this.maxAttributes;
		}

		public void setMaxAttributes(int maxAttributes) {
			this.maxAttributes = maxAttributes;
		}

		public int getMaxEvents() {
			return this.maxEvents;
		}

		public void setMaxEvents(int maxEvents) {
			this.maxEvents = maxEvents;
		}

		public int getMaxLinks() {
			return this.maxLinks;
		}

		public void setMaxLinks(int maxLinks) {
			this.maxLinks = maxLinks;
		}

		public int getMaxAttributesPerEvent() {
			return this.maxAttributesPerEvent;
		}

		public void setMaxAttributesPerEvent(int maxAttributesPerEvent) {
			this.maxAttributesPerEvent = maxAttributesPerEvent;
		}

		public int getMaxAttributesPerLink() {
			return this.maxAttributesPerLink;
		}

		public void setMaxAttributesPerLink(int maxAttributesPerLink) {
			this.maxAttributesPerLink = maxAttributesPerLink;
		}

	}

	/**
	 * Supported samplers.
	 */
	public enum Sampler {

		ALWAYS_ON, ALWAYS_OFF, TRACE_ID_RATIO, PARENT_BASED_ALWAYS_ON, PARENT_BASED_ALWAYS_OFF,
		PARENT_BASED_TRACE_ID_RATIO

	}

}
