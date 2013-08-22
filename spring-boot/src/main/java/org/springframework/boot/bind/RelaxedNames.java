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
import java.util.NoSuchElementException;

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

	/**
	 * Create a new {@link RelaxedNames} instance.
	 * 
	 * @param name the source name. For the maximum number of variations specify the name
	 * using dashed notation (e.g. {@literal my-property-name}
	 */
	public RelaxedNames(String name) {
		this.name = name;
	}

	@Override
	public Iterator<String> iterator() {
		return new RelaxedNamesIterator();
	}

	private class RelaxedNamesIterator implements Iterator<String> {

		private int variation = 0;

		private int manipulation = 0;

		@Override
		public boolean hasNext() {
			return (this.variation < Variation.values().length);
		}

		@Override
		public String next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			String result = RelaxedNames.this.name;
			result = Manipulation.values()[this.manipulation].apply(result);
			result = Variation.values()[this.variation].apply(result);
			this.manipulation++;
			if (this.manipulation >= Manipulation.values().length) {
				this.variation++;
				this.manipulation = 0;
			}
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
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
		UNDERSCORE {
			@Override
			public String apply(String value) {
				return value.replace("-", "_");
			}
		},
		CAMELCASE {
			@Override
			public String apply(String value) {
				StringBuilder builder = new StringBuilder();
				for (String field : UNDERSCORE.apply(value).split("_")) {
					builder.append(builder.length() == 0 ? field : StringUtils
							.capitalize(field));
				}
				return builder.toString();
			}
		};

		public abstract String apply(String value);
	}
}
