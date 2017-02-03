/*
 *
 *  * Copyright 2012-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.boot.env.fixme;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class PropertiesCanonicalPropertySource extends MapPropertySource {

	private Map<String, Object> canonicalProperties = new HashMap<>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public PropertiesCanonicalPropertySource(String name, Properties source) {
		super(name, (Map) source);
		this.canonicalProperties = toCanonicalProperties(source);
	}

	@Override
	public boolean containsProperty(String name) {
		return super.containsProperty(name) || this.canonicalProperties.containsKey(name);
	}

	@Override
	public Object getProperty(String name) {
		if (super.containsProperty(name)) {
			return super.getProperty(name);
		}
		return this.canonicalProperties.get(name);
	}

	public Set<String> getCanonicalPropertyNames() {
		return this.canonicalProperties.keySet();
	}

	private Map<String, Object> toCanonicalProperties(Properties properties) {
		Map<String, Object> map = new HashMap<>();
		for (Map.Entry entry : properties.entrySet()) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			if (key.endsWith("[]")) {
				String prefix = StringUtils.delimitedListToStringArray(key, "[]")[0];
				addIndividualArrayElements(map, prefix, value);
			}
			else {
				map.put(toCanonicalName(key), entry.getValue());
			}
		}
		return map;
	}

	private void addIndividualArrayElements(Map<String, Object> map, String key,
			String value) {
		String keyPrefix = toCanonicalName(key);
		String[] values = StringUtils.commaDelimitedListToStringArray(value);
		for (int i = 0; i < values.length; i++) {
			map.put(keyPrefix + "[" + i + "]", values[i]);
		}
	}

	private String toCanonicalName(String key) {
		StringBuilder result = new StringBuilder(key.length());
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '.' || c == '[' || c == ']') {
				result.append(c);
			}
		}
		return result.toString().toLowerCase();
	}

}
