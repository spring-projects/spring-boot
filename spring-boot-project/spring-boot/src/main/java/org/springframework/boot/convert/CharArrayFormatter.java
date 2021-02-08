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

package org.springframework.boot.convert;

import java.text.ParseException;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} for {@code char[]}.
 *
 * @author Phillip Webb
 */
final class CharArrayFormatter implements Formatter<char[]> {

	@Override
	public String print(char[] object, Locale locale) {
		return new String(object);
	}

	@Override
	public char[] parse(String text, Locale locale) throws ParseException {
		return text.toCharArray();
	}

}
