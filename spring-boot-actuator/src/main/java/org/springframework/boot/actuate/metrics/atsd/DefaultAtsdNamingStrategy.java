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

package org.springframework.boot.actuate.metrics.atsd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default ATSD naming strategy implementation.
 *
 * @author Alexander Tokarev.
 */
public class DefaultAtsdNamingStrategy implements AtsdNamingStrategy, InitializingBean {
	/**
	 * Default entity name.
	 */
	public static final String DEFAULT_ENTITY = "atsd-default";
	private Map<String, AtsdName> cache = new ConcurrentHashMap<String, AtsdName>();
	private String entity = DEFAULT_ENTITY;
	private String metricPrefix;
	private Map<String, String> tags = new HashMap<String, String>();

	/**
	 * ATSD requires an entity name.
	 * Entities are servers, hosts, frames, virtual machines, or sensors.
	 * The default value is 'atsd-default'
	 * @param entity the name of the entity.
	 */
	public void setEntity(String entity) {
		this.entity = entity;
	}

	/**
	 * Prefix to be prepended to the original metric name.
	 * @param metricPrefix the prefix
	 */
	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}

	/**
	 * Optional set of key - value pairs in time series identifier.
	 * @param tags the tags map
	 * @see <a href="https://axibase.com/products/axibase-time-series-database/data-model/">ATSD Data Model</a>
	 */
	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public Map<String, String> getTags() {
		return this.tags;
	}

	@Override
	public AtsdName getName(String metricName) {
		if (this.cache.containsKey(metricName)) {
			return this.cache.get(metricName);
		}
		String metric = StringUtils.isEmpty(this.metricPrefix) ? metricName : this.metricPrefix + metricName;
		AtsdName atsdName = new AtsdName(metric, this.entity, this.tags);
		this.cache.put(metricName, atsdName);
		return atsdName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.entity, "Entity is required");
		if (this.tags != null) {
			for (Map.Entry<String, String> tagKeyValue : this.tags.entrySet()) {
				Assert.hasText(tagKeyValue.getKey(), "Empty tag key: " + this.tags);
				Assert.hasText(tagKeyValue.getValue(), "Empty tag value: " + this.tags);
			}
		}
	}
}
