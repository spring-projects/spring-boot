/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class that can be used to filter source data using a name regular expression.
 * Detects if the name is classic "single value" key or a regular expression. Subclasses
 * must provide implementations of {@link #getValue(Object, String)} and
 * {@link #getNames(Object, NameCallback)}.
 *
 * @param <T> the source data type
 * @author Phillip Webb
 * @author Sergei Egorov
 * @author Andy Wilkinson
 * @author Dylian Bego
 * @since 1.3.0
 */
abstract class NamePatternFilter<T> {

	private static final String[] REGEX_PARTS = { "*", "$", "^", "+", "[" };

	private final T source;

	NamePatternFilter(T source) {
		this.source = source;
	}

	public Map<String, Object> getResults(String name) {
		Pattern pattern = compilePatternIfNecessary(name);
		if (pattern == null) {
			Object value = getValue(this.source, name);
			Map<String, Object> result = new HashMap<String, Object>();
			result.put(name, value);
			return result;
		}
		ResultCollectingNameCallback resultCollector = new ResultCollectingNameCallback(
				pattern);
		getNames(this.source, resultCollector);
		return resultCollector.getResults();

	}

	private Pattern compilePatternIfNecessary(String name) {
		for (String part : REGEX_PARTS) {
			if (name.contains(part)) {
				try {
					return Pattern.compile(name);
				}
				catch (PatternSyntaxException ex) {
					return null;
				}
			}
		}
		return null;
	}

	protected abstract void getNames(T source, NameCallback callback);

	protected abstract Object getValue(T source, String name);

	protected abstract Object getOptionalValue(T source, String name);

	/**
	 * Callback used to add a name.
	 */
	protected interface NameCallback {

		void addName(String name);

	}

	/**
	 * {@link NameCallback} implementation to collect results.
	 */
	private class ResultCollectingNameCallback implements NameCallback {

		private final Pattern pattern;

		private final Map<String, Object> results = new LinkedHashMap<String, Object>();

		ResultCollectingNameCallback(Pattern pattern) {
			this.pattern = pattern;
		}

		@Override
		public void addName(String name) {
			if (this.pattern.matcher(name).matches()) {
				Object value = getOptionalValue(NamePatternFilter.this.source, name);
				if (value != null) {
					this.results.put(name, value);
				}
			}
		}

		public Map<String, Object> getResults() {
			return this.results;
		}

	}

}
