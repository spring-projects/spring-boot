/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.jetty.util;

import java.util.Arrays;

/**
 * StringUtil class.
 */
public final class StringUtil {

	/**
     * Private constructor for the StringUtil class.
     */
    private StringUtil() {
	}

	/**
     * Repeats a given character a specified number of times and returns the resulting string.
     * 
     * @param c the character to be repeated
     * @param length the number of times the character should be repeated
     * @return the resulting string with the repeated character
     */
    public static String repeat(char c, int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, c);
		return new String(chars);
	}

}
