/*
 * Copyright 2012-2019 the original author or authors.
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
 * Internal utility to help when dealing with Java Bean property names.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class BeanPropertyName {

	private BeanPropertyName() {
	}

	/**
	 * Return the specified Java Bean property name in dashed form.
	 * @param name the source name
	 * @return the dashed from
	 */
	public static String toDashedForm(String name) {
		return toDashedForm(name, 0);
	}

	/**
	 * Return the specified Java Bean property name in dashed form.
	 * @param name the source name
	 * @param start the starting char
	 * @return the dashed from
	 */
	public static String toDashedForm(String name, int start) {
		StringBuilder result = new StringBuilder();
		char[] chars = name.replace("_", "-").toCharArray();
		for (int i = start; i < chars.length; i++) {
			char ch = chars[i];
			if (Character.isUpperCase(ch) && result.length() > 0 && result.charAt(result.length() - 1) != '-') {
				result.append("-");
			}
			result.append(Character.toLowerCase(ch));
		}
		return result.toString();
	}

}
