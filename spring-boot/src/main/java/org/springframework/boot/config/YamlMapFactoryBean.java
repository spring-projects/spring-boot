/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.boot.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;

/**
 * Factory for Map that reads from a YAML source. YAML is a nice human-readable format for
 * configuration, and it has some useful hierarchical properties. It's more or less a
 * superset of JSON, so it has a lot of similar features. If multiple resources are
 * provided the later ones will override entries in the earlier ones hierarchically - that
 * is all entries with the same nested key of type Map at any depth are merged. For
 * example:
 * 
 * <pre>
 * foo:
 *   bar:
 *    one: two
 * three: four
 * 
 * </pre>
 * 
 * plus (later in the list)
 * 
 * <pre>
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * 
 * </pre>
 * 
 * results in an effecive input of
 * 
 * <pre>
 * foo:
 *   bar:
 *    one: 2
 *    three: four
 * five: six
 * 
 * </pre>
 * 
 * Note that the value of "foo" in the first document is not simply replaced with the
 * value in the second, but its nested values are merged.
 * 
 * @author Dave Syer
 */
public class YamlMapFactoryBean extends YamlProcessor implements
		FactoryBean<Map<String, Object>> {

	private boolean singleton = true;

	private Map<String, Object> instance;

	@Override
	public Map<String, Object> getObject() {
		if (!this.singleton || this.instance == null) {
			final Map<String, Object> result = new LinkedHashMap<String, Object>();
			process(new MatchCallback() {
				@Override
				public void process(Properties properties, Map<String, Object> map) {
					merge(result, map);
				}
			});
			this.instance = result;
		}
		return this.instance;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void merge(Map<String, Object> output, Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Object existing = output.get(key);
			if (value instanceof Map && existing instanceof Map) {
				Map<String, Object> result = new LinkedHashMap<String, Object>(
						(Map) existing);
				merge(result, (Map) value);
				output.put(key, result);
			}
			else {
				output.put(key, value);
			}
		}
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	/**
	 * Set if a singleton should be created, or a new object on each request otherwise.
	 * Default is <code>true</code> (a singleton).
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

}
