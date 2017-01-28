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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Madhura Bhave
 */
public class CanonicalSystemEnvironmentPropertySource extends MapPropertySource {

	private final Map<String, String> canonicalMap;


	public CanonicalSystemEnvironmentPropertySource(String name, Map<String, Object> source) {
		super(name, source);
		this.canonicalMap = Collections.unmodifiableMap(source.keySet().stream().collect(Collectors.toMap(this::toCanonicalName, key -> key)));
	}

	@Override
	public Object getProperty(String name) {
		if (super.containsProperty(name)) {
			return super.getProperty(name);
		}
		String canonicalName = canonicalMap.get(name);
		return (canonicalName == null ? null : super.getProperty(canonicalName));
	}

	@Override
	public boolean containsProperty(String name) {
		return super.containsProperty(name) || canonicalMap.containsKey(name);
	}

	private String toCanonicalName(String key) {
		Stream<String> stream = Arrays.stream(StringUtils.delimitedListToStringArray(key.toLowerCase(), "_"));
		List<String> elements = stream.filter(element -> !element.isEmpty()).collect(Collectors.toList());
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < elements.size(); i++) {
			if (isNumber(elements.get(i))) {
				result.append("[" + elements.get(i) + "]");
			}
			else {
				if (i != 0) {
					result.append(".");
				}
				result.append(elements.get(i));
			}
		}
		return result.toString();
	}

	//stream version
	private String toCanonicalName2(String key) {
		return Arrays.stream(StringUtils.delimitedListToStringArray(key.toLowerCase(), "_"))
					.filter(element -> !element.isEmpty())
						.map(element -> isNumber(element) ? "[" + element + "]" : element)
							.collect(Collectors.joining(".")).replace(".[", "[");
	}

	private boolean isNumber(String element) {
		for (char c : element.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	public Set<String> getCanonicalPropertyNames() {
		return canonicalMap.keySet();
	}

}
