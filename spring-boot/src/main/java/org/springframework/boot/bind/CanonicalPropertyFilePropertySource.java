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

package org.springframework.boot.bind;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Madhura Bhave
 */
public class CanonicalPropertyFilePropertySource extends PropertiesPropertySource {

	private Map<String, Object> canonicalProperties = new HashMap<>();


	public CanonicalPropertyFilePropertySource(String name, Properties properties) {
		super(name, properties);
		this.canonicalProperties = toCanonicalProperties(properties);
	}

	public boolean containsProperty(String name) {
		return super.containsProperty(name) || this.canonicalProperties.containsKey(name);
	}

	public Object getProperty(String name) {
		if (super.containsProperty(name)) {
			return super.getProperty(name);
		}
		return canonicalProperties.get(name);
	}

	public Set<String> getCanonicalPropertyNames() {
		return canonicalProperties.keySet();
	}

	private Map<String, Object> toCanonicalProperties(Properties properties) {
		Map<String,Object> map = new HashMap<>();
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

	private void addIndividualArrayElements(Map<String, Object> map, String key, String value) {
		String keyPrefix = toCanonicalName(key);
		String[] values = StringUtils.commaDelimitedListToStringArray(value);
		for (int i = 0; i < values.length; i++) {
			map.put(keyPrefix + "[" + i + "]", values[i]);
		}
	}

	private String toCanonicalName(String key) {
		StringBuilder result = new StringBuilder(key.length());
		for (int i=0;i<key.length();i++) {
			char c = key.charAt(i);
			if(Character.isLetterOrDigit(c) || c == '.' || c == '[' || c == ']') {
				result.append(c);
			}
		}
		return result.toString().toLowerCase();
	}

}
