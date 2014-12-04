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

package org.springframework.boot.configurationprocessor;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;

/**
 * Filter to excluded elements that don't make sense to process.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class ElementExcludeFilter {

	private final Set<String> excludes = new HashSet<String>();

	public ElementExcludeFilter() {
		add("java.io.Writer");
		add("java.io.PrintWriter");
		add("javax.sql.DataSource");
		add("java.lang.ClassLoader");
	}

	private void add(String className) {
		this.excludes.add(className);
	}

	public boolean isExcluded(Element element) {
		if (element == null) {
			return false;
		}
		return this.excludes.contains(element.toString());
	}

}
