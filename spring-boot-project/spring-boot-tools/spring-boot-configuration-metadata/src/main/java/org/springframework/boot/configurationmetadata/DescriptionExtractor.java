/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationmetadata;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Utility to extract a description.
 *
 * @author Stephane Nicoll
 */
class DescriptionExtractor {

	private static final String NEW_LINE = System.getProperty("line.separator");

	public String getShortDescription(String description) {
		if (description == null) {
			return null;
		}
		int dot = description.indexOf(".");
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(description);
			String text = description
					.substring(breakIterator.first(), breakIterator.next()).trim();
			return removeSpaceBetweenLine(text);
		}
		else {
			String[] lines = description.split(NEW_LINE);
			return lines[0].trim();
		}
	}

	private String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(NEW_LINE);
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line.trim()).append(" ");
		}
		return sb.toString().trim();
	}

}
