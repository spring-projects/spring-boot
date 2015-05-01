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

package org.springframework.boot.actuate.metrics.opentsdb;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ObjectUtils;

/**
 * A naming strategy that just passes through the metric name, together with tags from a
 * set of static values. Open TSDB requires at least one tag, so one is always added for
 * you: the {@value #PREFIX_KEY} key is added with a unique value "spring.X" where X is an
 * object hash code ID for this (the naming stategy). In most cases this will be unique
 * enough to allow aggregation of the underlying metrics in Open TSDB, but normally it is
 * best to provide your own tags, including a prefix if you know one (overwriting the
 * default).
 *
 * @author Dave Syer
 */
public class DefaultOpenTsdbNamingStrategy implements OpenTsdbNamingStrategy {

	public static final String PREFIX_KEY = "prefix";

	/**
	 * Tags to apply to every metric. Open TSDB requires at least one tag, so a "prefix"
	 * tag is added for you by default.
	 */
	private Map<String, String> tags = new LinkedHashMap<String, String>();

	private Map<String, OpenTsdbName> cache = new HashMap<String, OpenTsdbName>();

	public DefaultOpenTsdbNamingStrategy() {
		this.tags.put(PREFIX_KEY,
				"spring." + ObjectUtils.getIdentityHexString(this));
	}

	public void setTags(Map<String, String> staticTags) {
		this.tags.putAll(staticTags);
	}

	@Override
	public OpenTsdbName getName(String name) {
		if (this.cache.containsKey(name)) {
			return this.cache.get(name);
		}
		OpenTsdbName value = new OpenTsdbName(name);
		value.setTags(this.tags);
		this.cache.put(name, value);
		return value;
	}

}
