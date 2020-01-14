/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

/**
 * Internal utility to help when dealing with data object property names.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.2.3
 * @see DataObjectBinder
 */
public abstract class DataObjectPropertyName {

	private DataObjectPropertyName() {
	}

	/**
	 * Return the specified Java Bean property name in dashed form.
	 * @param name the source name
	 * @return the dashed from
	 */
	public static String toDashedForm(String name) {
		StringBuilder result = new StringBuilder();
		String replaced = name.replace('_', '-');
		for (int i = 0; i < replaced.length(); i++) {
			char ch = replaced.charAt(i);
			if (Character.isUpperCase(ch) && result.length() > 0 && result.charAt(result.length() - 1) != '-') {
				result.append('-');
			}
			result.append(Character.toLowerCase(ch));
		}
		return result.toString();
	}

}
