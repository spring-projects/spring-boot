/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * Generates relaxed name variations from a given source.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see RelaxedDataBinder
 * @see RelaxedPropertyResolver
 */
public final class RelaxedNames implements Iterable<String> {

	private final String name;

	private Set<String> values = new LinkedHashSet<String>();

	/**
	 * Create a new {@link RelaxedNames} instance.
	 * 
	 * @param name the source name. For the maximum number of variations specify the name
	 * using dashed notation (e.g. {@literal my-property-name}
	 */
	public RelaxedNames(String name) {
		this.name = name;
		initialize(RelaxedNames.this.name, this.values);
	}

	@Override
	public Iterator<String> iterator() {
		return this.values.iterator();
	}

	private void initialize(String name, Set<String> values) {
		if (values.contains(name)) {
			return;
		}
		for (Variation variation : Variation.values()) {
			for (Manipulation manipulation : Manipulation.values()) {
				String result = name;
				result = manipulation.apply(result);
				result = variation.apply(result);
				values.add(result);
				initialize(result, values);
			}
		}
	}

	static enum Variation {
		NONE {
			@Override
			public String apply(String value) {
				return value;
			}
		},
		LOWERCASE {
			@Override
			public String apply(String value) {
				return value.toLowerCase();
			}
		},
		UPPERCASE {
			@Override
			public String apply(String value) {
				return value.toUpperCase();
			}
		};

		public abstract String apply(String value);
	}

	static enum Manipulation {
		NONE {
			@Override
			public String apply(String value) {
				return value;
			}
		},
		HYPHEN_TO_UNDERSCORE {
			@Override
			public String apply(String value) {
				return value.replace("-", "_");
			}
		},
		PERIOD_TO_UNDERSCORE {
			@Override
			public String apply(String value) {
				return value.replace(".", "_");
			}
		},
		CAMELCASE_TO_UNDERSCORE {
			@Override
			public String apply(String value) {
				value = value.replaceAll("([^A-Z-])([A-Z])", "$1_$2");
				StringBuilder builder = new StringBuilder();
				for (String field : value.split("_")) {
					if (builder.length() == 0) {
						builder.append(field);
					}
					else {
						builder.append("_").append(StringUtils.uncapitalize(field));
					}
				}
				return builder.toString();
			}
		},
		SEPARATED_TO_CAMELCASE {
			@Override
			public String apply(String value) {
				StringBuilder builder = new StringBuilder();
				for (String field : value.split("[_\\-.]")) {
					builder.append(builder.length() == 0 ? field : StringUtils
							.capitalize(field));
				}
				for (String suffix : new String[] { "_", "-", "." })
					if (value.endsWith(suffix)) {
						builder.append(suffix);
					}
				return builder.toString();
			}
		};

		public abstract String apply(String value);
	}
}
