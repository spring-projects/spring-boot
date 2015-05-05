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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Dave Syer
 */
@ConfigurationProperties("spring.metrics")
public class MetricsProperties {

	private Export export = new Export();

	public Export getExport() {
		return this.export;
	}

	public static class Export {
		/**
		 * Delay in milliseconds between export ticks. Metrics are exported to external
		 * sources on a schedule with this delay.
		 */
		private long delayMillis;

		/**
		 * Flag to enable metric export (assuming a MetricWriter is available).
		 */
		private boolean enabled = true;

		/**
		 * Flag to switch off any available optimizations based on not exporting unchanged
		 * metric values.
		 */
		private boolean ignoreTimestamps = false;

		private String[] includes;

		private String[] excludes;

		public String[] getIncludes() {
			return this.includes;
		}

		public void setIncludes(String[] includes) {
			this.includes = includes;
		}

		public String[] getExcludes() {
			return this.excludes;
		}

		public void setExcludes(String[] excludes) {
			this.excludes = excludes;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public long getDelayMillis() {
			return this.delayMillis;
		}

		public void setDelayMillis(long delayMillis) {
			this.delayMillis = delayMillis;
		}

		public boolean isIgnoreTimestamps() {
			return this.ignoreTimestamps;
		}

		public void setIgnoreTimestamps(boolean ignoreTimestamps) {
			this.ignoreTimestamps = ignoreTimestamps;
		}
	}
}
