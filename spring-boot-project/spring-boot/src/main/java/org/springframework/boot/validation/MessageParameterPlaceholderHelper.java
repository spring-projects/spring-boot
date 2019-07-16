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

package org.springframework.boot.validation;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class to extract message parameters ({@code {param}}) from the message. These
 * parameters can be substituted for supplied values.
 *
 * @author Dmytro Nosan
 */
final class MessageParameterPlaceholderHelper {

	private static final char PREFIX = '{';

	private static final char SUFFIX = '}';

	private static final char ESCAPE = '\\';

	/**
	 * Replaces all message parameters using the given {@code parameterResolver}.
	 * <p>
	 * If returned value has other message parameters, they will be replaced recursively
	 * until no replacement is performed;
	 * <p>
	 * Resolver can return {@code null} to signal that no further actions need to be done
	 * and replacement should be omitted;
	 * <p>
	 * The message parameter can be escaped by the {@code '\'} symbol;
	 * @param message the value containing the parameters to be replaced
	 * @param parameterResolver the {@code parameterResolver} to use for replacement
	 * @return the replaced message
	 */
	String replaceParameters(String message, Function<String, String> parameterResolver) {
		return replaceParameters(message, parameterResolver, new LinkedHashSet<>(4));
	}

	private static String replaceParameters(String message, Function<String, String> parameterResolver,
			Set<String> visitedParameters) {
		StringBuilder buf = new StringBuilder(message);
		int parentheses = 0;
		int startIndex = -1;
		int endIndex = -1;
		for (int i = 0; i < buf.length(); i++) {
			if (buf.charAt(i) == ESCAPE) {
				i++;
			}
			else if (buf.charAt(i) == PREFIX) {
				if (startIndex == -1) {
					startIndex = i;
				}
				parentheses++;
			}
			else if (buf.charAt(i) == SUFFIX) {
				if (parentheses > 0) {
					parentheses--;
				}
				endIndex = i;
			}
			if (parentheses == 0 && startIndex < endIndex) {
				String parameter = buf.substring(startIndex + 1, endIndex);
				if (!visitedParameters.add(parameter)) {
					throw new IllegalArgumentException("Circular reference '{" + parameter + "}'");
				}
				String value = replaceParameter(parameter, parameterResolver, visitedParameters);
				if (value != null) {
					buf.replace(startIndex, endIndex + 1, value);
					i = startIndex + value.length() - 1;
				}
				visitedParameters.remove(parameter);
				startIndex = -1;
				endIndex = -1;
			}
		}
		return buf.toString();
	}

	private static String replaceParameter(String parameter, Function<String, String> parameterResolver,
			Set<String> visitedParameters) {
		parameter = replaceParameters(parameter, parameterResolver, visitedParameters);
		String value = parameterResolver.apply(parameter);
		if (value != null) {
			return replaceParameters(value, parameterResolver, visitedParameters);
		}
		return null;
	}

}
