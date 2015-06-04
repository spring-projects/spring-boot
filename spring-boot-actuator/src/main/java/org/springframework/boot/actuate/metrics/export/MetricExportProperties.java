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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for metrics export.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ConfigurationProperties("spring.metrics.export")
public class MetricExportProperties extends TriggerProperties {

	/**
	 * Flag to disable all metric exports (assuming any MetricWriters are available).
	 */
	private boolean enabled = true;

	private Map<String, SpecificTriggerProperties> triggers = new LinkedHashMap<String, SpecificTriggerProperties>();

	private Redis redis = new Redis();

	@PostConstruct
	public void setUpDefaults() {
		TriggerProperties defaults = this;
		for (Entry<String, SpecificTriggerProperties> entry : this.triggers.entrySet()) {
			String key = entry.getKey();
			SpecificTriggerProperties value = entry.getValue();
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
		for (TriggerProperties value : this.triggers.values()) {
			if (value.isSendLatest() == null) {
				value.setSendLatest(defaults.isSendLatest());
			}
			if (value.getDelayMillis() == null) {
				value.setDelayMillis(defaults.getDelayMillis());
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Configuration for triggers on individual named writers. Each value can individually
	 * specify a name pattern explicitly, or else the map key will be used if the name is
	 * not set.
	 * @return the writers
	 */
	public Map<String, SpecificTriggerProperties> getTriggers() {
		return this.triggers;
	}

	public Redis getRedis() {
		return this.redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

	/**
	 * Default values for trigger configuration for all writers. Can also be set by
	 * including a writer with {@code name="*"}.
	 * @return the default trigger configuration
	 */
	public TriggerProperties getDefault() {
		return this;
	}

	/**
	 * Find a matching trigger configuration.
	 * @param name the bean name to match
	 * @return a matching configuration if there is one
	 */
	public TriggerProperties findTrigger(String name) {
		for (SpecificTriggerProperties value : this.triggers.values()) {
			if (PatternMatchUtils.simpleMatch(value.getNames(), name)) {
				return value;
			}
		}
		return this;
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
		private String prefix = "spring.metrics";

		/**
		 * Key for redis repository export (if active). Should be globally unique for a
		 * system sharing a redis repository.
		 */
		private String key = "keys.spring.metrics";

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getKey() {
			return this.key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getAggregatePrefix() {
			String[] tokens = StringUtils.delimitedListToStringArray(this.prefix, ".");
			if (tokens.length > 1) {
				if (StringUtils.hasText(tokens[1])) {
					// If the prefix has 2 or more non-trivial parts, use the first 1
					// (the aggregator strips a further 2 by default).
					return tokens[0];
				}
			}
			return this.prefix;
		}

	}

}
