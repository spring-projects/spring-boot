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

/**
 * Configuration properties for metrics export.
 *
 * @author Dave Syer
 * @author Simon Buettner
 * @since 1.3.0
 */
@ConfigurationProperties("spring.metrics.export")
public class MetricExportProperties extends TriggerProperties {

	/**
	 * Specific trigger properties per MetricWriter bean name.
	 */
	private Map<String, SpecificTriggerProperties> triggers = new LinkedHashMap<String, SpecificTriggerProperties>();

	private Aggregate aggregate = new Aggregate();

	private Redis redis = new Redis();

	private Statsd statsd = new Statsd();

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

	/**
	 * Configuration for triggers on individual named writers. Each value can individually
	 * specify a name pattern explicitly, or else the map key will be used if the name is
	 * not set.
	 * @return the writers
	 */
	public Map<String, SpecificTriggerProperties> getTriggers() {
		return this.triggers;
	}

	public Aggregate getAggregate() {
		return this.aggregate;
	}

	public void setAggregate(Aggregate aggregate) {
		this.aggregate = aggregate;
	}

	public Redis getRedis() {
		return this.redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

	public Statsd getStatsd() {
		return this.statsd;
	}

	public void setStatsd(Statsd statsd) {
		this.statsd = statsd;
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

	/**
	 * Aggregate properties.
	 */
	public static class Aggregate {

		/**
		 * Prefix for global repository if active. Should be unique for this JVM, but most
		 * useful if it also has the form "a.b" where "a" is unique to this logical
		 * process (this application) and "b" is unique to this physical process. If you
		 * set spring.application.name elsewhere, then the default will be in the right
		 * form.
		 */
		private String prefix = "";

		/**
		 * Pattern that tells the aggregator what to do with the keys from the source
		 * repository. The keys in the source repository are assumed to be period
		 * separated, and the pattern is in the same format, e.g. "d.d.k.d". Here "d"
		 * means "discard" and "k" means "keep" the key segment in the corresponding
		 * position in the source.
		 */
		private String keyPattern = "";

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getKeyPattern() {
			return this.keyPattern;
		}

		public void setKeyPattern(String keyPattern) {
			this.keyPattern = keyPattern;
		}

	}

	/**
	 * Redis properties.
	 */
	public static class Redis {

		/**
		 * Prefix for redis repository if active. Should be globally unique across all
		 * processes sharing the same repository.
		 */
		private String prefix = "spring.metrics";

		/**
		 * Key for redis repository export (if active). Should be globally unique for a
		 * system sharing a redis repository across multiple processes.
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
			// The common case including a standalone aggregator would have a prefix that
			// starts with the end of the key, so strip that bit off and call it the
			// aggregate prefix.
			if (this.key.startsWith("keys.")) {
				String candidate = this.key.substring("keys.".length());
				if (this.prefix.startsWith(candidate)) {
					return candidate;
				}
				return candidate;
			}
			// If the user went off piste, choose something that is safe (not empty) but
			// not the whole prefix (on the assumption that it contains dimension keys)
			if (this.prefix.contains(".")
					&& this.prefix.indexOf(".") < this.prefix.length() - 1) {
				return this.prefix.substring(this.prefix.indexOf(".") + 1);
			}
			return this.prefix;
		}

	}

	/**
	 * Statsd properties.
	 */
	public static class Statsd {

		/**
		 * Host of a statsd server to receive exported metrics.
		 */
		private String host;

		/**
		 * Port of a statsd server to receive exported metrics.
		 */
		private int port = 8125;

		/**
		 * Prefix for statsd exported metrics.
		 */
		private String prefix;

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

	}

}
