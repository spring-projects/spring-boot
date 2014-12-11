/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration meta-data.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ItemMetadata
 */
public class ConfigurationMetadata {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final List<ItemMetadata> items;

	public ConfigurationMetadata() {
		this.items = new ArrayList<ItemMetadata>();
	}

	public ConfigurationMetadata(ConfigurationMetadata metadata) {
		this.items = new ArrayList<ItemMetadata>(metadata.getItems());
	}

	/**
	 * Add item meta-data.
	 * @param itemMetadata the meta-data to add
	 */
	public void add(ItemMetadata itemMetadata) {
		this.items.add(itemMetadata);
		Collections.sort(this.items);
	}

	/**
	 * Add all properties from another {@link ConfigurationMetadata}.
	 * @param metadata the {@link ConfigurationMetadata} instance to merge
	 */
	public void addAll(ConfigurationMetadata metadata) {
		this.items.addAll(metadata.getItems());
		Collections.sort(this.items);
	}

	/**
	 * @return the meta-data properties.
	 */
	public List<ItemMetadata> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	public static String nestedPrefix(String prefix, String name) {
		String nestedPrefix = (prefix == null ? "" : prefix);
		String dashedName = toDashedCase(name);
		nestedPrefix += ("".equals(nestedPrefix) ? dashedName : "." + dashedName);
		return nestedPrefix;
	}

	static String toDashedCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String first = matcher.group(1);
			String second = matcher.group(2);
			String target;
			if (first.equals("_")) { // not a word for the binder
				target = first + second;
			} else {
				target = first + "-" + second;
			}
			matcher.appendReplacement(result,target);
		}
		matcher.appendTail(result);
		String value = result.toString();
		return value.toLowerCase();
	}

}
