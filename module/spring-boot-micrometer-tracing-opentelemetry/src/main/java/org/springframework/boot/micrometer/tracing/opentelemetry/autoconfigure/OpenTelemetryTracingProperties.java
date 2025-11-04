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

	public Export getExport() {
		return this.export;
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

}
