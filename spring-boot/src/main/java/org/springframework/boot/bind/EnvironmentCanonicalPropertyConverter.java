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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Madhura Bhave
 */
public class EnvironmentCanonicalPropertyConverter implements CanonicalPropertyConverter {

	private final String NUMBER_REGEX = ".*_\\d+.*";
	//private final String DOUBLE_UNDERSCORE_REGEX = ".*?__[^_]+";

	@Override
	public String convert(String property) {
		if (property.matches(NUMBER_REGEX)) {
			property = surroundNumberWithBrackets(property);
		}

		return property.replace("_", ".").toLowerCase();
	}

	private String surroundNumberWithBrackets(String property) {
		Matcher matcher = Pattern.compile(".*?(_\\d+)").matcher(property);
		while (matcher.find()) {
			StringBuilder builder = new StringBuilder();
			String matched = matcher.group(1);
			String replaced = matched.replace("_", "[");
			builder.append(replaced).append("]");
			property = property.replace(matched, builder.toString());
		}
		if (property.lastIndexOf("_") == property.length() -1) {
			property = property.substring(0, property.length() - 1);
		}
		return property;
	}

	private String surroundStringWithBrackets(String property) {
//		Matcher matcher = Pattern.compile(".*?(__(.*?)__+)").matcher(property);
//		while (matcher.find()) {
//			String matched = matcher.group(1);
//			String replaced = matched.replaceFirst("__", "[").replace("__", "]_");
//			property = property.replace(matched, replaced);
//		}
//		return property;
	}

}