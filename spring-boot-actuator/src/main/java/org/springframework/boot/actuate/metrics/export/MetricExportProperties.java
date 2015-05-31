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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 */
@ConfigurationProperties("spring.metrics.export")
public class MetricExportProperties extends Trigger {

	/**
	 * Flag to disable all metric exports (assuming any MetricWriters are available).
	 */
	private boolean enabled = true;

	private Map<String, SpecificTrigger> triggers = new LinkedHashMap<String, SpecificTrigger>();

	private Redis redis = new Redis();

	/**
	 * Default values for trigger configuration for all writers. Can also be set by
	 * including a writer with <code>name="*"</code>.
	 *
	 * @return the default trigger configuration
	 */
	public Trigger getDefault() {
		return this;
	}

	/**
	 * Configuration for triggers on individual named writers. Each value can individually
	 * specify a name pattern explicitly, or else the map key will be used if the name is
	 * not set.
	 *
	 * @return the writers
	 */
	public Map<String, SpecificTrigger> getTriggers() {
		return this.triggers;
	}

	public Redis getRedis() {
		return redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

	@PostConstruct
	public void setUpDefaults() {
		Trigger defaults = this;
		for (Entry<String, SpecificTrigger> entry : this.triggers.entrySet()) {
			String key = entry.getKey();
			SpecificTrigger value = entry.getValue();
			if (value.getNames() == null || value.getNames().length == 0) {
				value.setNames(new String[] { key });
			}
		}
		if (defaults.isSendLatest() == null) {
			defaults.setSendLatest(true);
		}
		if (defaults.getDelayMillis() == null) {
			defaults.setDelayMillis(5000);
		}
		for (Trigger value : this.triggers.values()) {
			if (value.isSendLatest() == null) {
				value.setSendLatest(defaults.isSendLatest());
			}
			if (value.getDelayMillis() == null) {
				value.setDelayMillis(defaults.getDelayMillis());
			}
		}
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Find a matching trigger configuration.
	 * @param name the bean name to match
	 * @return a matching configuration if there is one
	 */
	public Trigger findTrigger(String name) {
		for (SpecificTrigger value : this.triggers.values()) {
			if (PatternMatchUtils.simpleMatch(value.getNames(), name)) {
				return value;
			}
		}
		return this;
	}
	
	/**
	 * Trigger for specific bean names.
	 */
	public static class SpecificTrigger extends Trigger {

		/**
		 * Names (or patterns) for bean names that this configuration applies to.
		 */
		private String[] names;

		public String[] getNames() {
			return this.names;
		}

		public void setNames(String[] names) {
			this.names = names;
		}

	}

	public static class Redis {

		/**
		 * Prefix for redis repository if active. Should be unique for this JVM, but most
		 * useful if it also has the form "x.y.a.b" where "x.y" is globally unique across
		 * all processes sharing the same repository, "a" is unique to this physical
		 * process and "b" is unique to this logical process (this application). If you
		 * set spring.application.name elsewhere, then the default will be in the right
		 * form.
		 */
		@Value("spring.metrics.${random.value:0000}.${spring.application.name:application}")
		private String prefix = "spring.metrics";

		/**
		 * Key for redis repository export (if active). Should be globally unique for a
		 * system sharing a redis repository.
		 */
		private String key = "keys.spring.metrics";

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getAggregatePrefix() {
			String[] tokens = StringUtils.delimitedListToStringArray(this.prefix, ".");
			if (tokens.length > 1) {
				if (StringUtils.hasText(tokens[1])) {
					// If the prefix has 2 or more non-trivial parts, use the first 1
					return tokens[0];
				}
			}
			return this.prefix;
		}

	}

}

class Trigger {

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
	 * List of patterns for metric names to include.
	 */
	private String[] includes;

	/**
	 * List of patterns for metric names to exclude. Applied after the includes.
	 */
	private String[] excludes;

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

	public Boolean isSendLatest() {
		return this.sendLatest;
	}

	public void setSendLatest(boolean sendLatest) {
		this.sendLatest = sendLatest;
	}

}
