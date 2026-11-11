/*
 * Copyright 2012-present the original author or authors.
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

	String getFirstSentence(String text) {
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
