/*
 * Copyright 2012-present the original author or authors.
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
