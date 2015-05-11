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

package org.springframework.boot.actuate.metrics.export;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.PatternMatchUtils;

/**
 * @author Dave Syer
 */
@ConfigurationProperties("spring.metrics.export")
public class MetricExportProperties {

	/**
	 * Flag to disable all metric exports (assuming any MetricWriters are available).
	 */
	private boolean enabled = true;

	private Export export = new Export();

	private Map<String, Export> writers = new LinkedHashMap<String, Export>();

	/**
	 * Default values for trigger configuration for all writers. Can also be set by
	 * including a writer with <code>name="*"</code>.
	 *
	 * @return the default trigger configuration
	 */
	public Export getDefault() {
		return this.export;
	}

	/**
	 * Configuration for triggers on individual named writers. Each value can individually
	 * specify a name pattern explicitly, or else the map key will be used if the name is
	 * not set.
	 *
	 * @return the writers
	 */
	public Map<String, Export> getWriters() {
		return this.writers;
	}

	@PostConstruct
	public void setDefaults() {
		Export defaults = null;
		for (Entry<String, Export> entry : this.writers.entrySet()) {
			String key = entry.getKey();
			Export value = entry.getValue();
			if (value.getNames() == null || value.getNames().length == 0) {
				value.setNames(new String[] { key });
			}
			if (Arrays.asList(value.getNames()).contains("*")) {
				defaults = value;
			}
		}
		if (defaults == null) {
			this.export.setNames(new String[] { "*" });
			this.writers.put("*", this.export);
			defaults = this.export;
		}
		if (defaults.isIgnoreTimestamps() == null) {
			defaults.setIgnoreTimestamps(false);
		}
		if (defaults.isSendLatest() == null) {
			defaults.setSendLatest(true);
		}
		if (defaults.getDelayMillis() == null) {
			defaults.setDelayMillis(5000);
		}
		for (Export value : this.writers.values()) {
			if (value.isIgnoreTimestamps() == null) {
				value.setIgnoreTimestamps(false);
			}
			if (value.isSendLatest() == null) {
				value.setSendLatest(true);
			}
			if (value.getDelayMillis() == null) {
				value.setDelayMillis(5000);
			}
		}
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public static class Export {
		/**
		 * Names (or patterns) for bean names that this configuration applies to.
		 */
		private String[] names;
		/**
		 * Delay in milliseconds between export ticks. Metrics are exported to external
		 * sources on a schedule with this delay.
		 */
		private Long delayMillis;

		/**
		 * Flag to enable metric export (assuming a MetricWriter is available).
		 */
		private boolean enabled = true;

		/**
		 * Flag to switch off any available optimizations based on not exporting unchanged
		 * metric values.
		 */
		private Boolean sendLatest;

		/**
		 * Flag to ignore timestamps completely. If true, send all metrics all the time,
		 * including ones that haven't changed since startup.
		 */
		private Boolean ignoreTimestamps;

		private String[] includes;

		private String[] excludes;

		public String[] getNames() {
			return this.names;
		}

		public void setNames(String[] names) {
			this.names = names;
		}

		public String[] getIncludes() {
			return this.includes;
		}

		public void setIncludes(String[] includes) {
			this.includes = includes;
		}

		public void setExcludes(String[] excludes) {
			this.excludes = excludes;
		}

		public String[] getExcludes() {
			return this.excludes;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Long getDelayMillis() {
			return this.delayMillis;
		}

		public void setDelayMillis(long delayMillis) {
			this.delayMillis = delayMillis;
		}

		public Boolean isIgnoreTimestamps() {
			return this.ignoreTimestamps;
		}

		public void setIgnoreTimestamps(boolean ignoreTimestamps) {
			this.ignoreTimestamps = ignoreTimestamps;
		}

		public Boolean isSendLatest() {
			return this.sendLatest;
		}

		public void setSendLatest(boolean sendLatest) {
			this.sendLatest = sendLatest;
		}
	}

	/**
	 * Find a matching trigger configuration.
	 * @param name the bean name to match
	 * @return a matching configuration if there is one
	 */
	public Export findExport(String name) {
		for (Export value : this.writers.values()) {
			if (PatternMatchUtils.simpleMatch(value.getNames(), name)) {
				return value;
			}
		}
		return null;
	}
}
