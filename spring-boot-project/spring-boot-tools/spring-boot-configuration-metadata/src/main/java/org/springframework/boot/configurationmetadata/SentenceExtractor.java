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

package org.springframework.boot.configurationmetadata;

import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility to extract the first sentence of a text.
 *
 * @author Stephane Nicoll
 */
class SentenceExtractor {

	public String getFirstSentence(String text) {
		if (text == null) {
			return null;
		}
		int dot = text.indexOf('.');
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(text);
			String sentence = text.substring(breakIterator.first(), breakIterator.next());
			return removeSpaceBetweenLine(sentence.trim());
		}
		else {
			String[] lines = text.split(System.lineSeparator());
			return lines[0].trim();
		}
	}

	private String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		return Arrays.stream(lines).map(String::trim).collect(Collectors.joining(" "));
	}

}
