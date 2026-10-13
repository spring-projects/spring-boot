/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Convention utilities.
 *
 * @author Stephane Nicoll
 * @since 3.4.0
 */
public abstract class ConventionUtils {

	private static final Set<Character> SEPARATORS;

	static {
		List<Character> chars = Arrays.asList('-', '_');
		SEPARATORS = Collections.unmodifiableSet(new HashSet<>(chars));
	}

	/**
	 * Return the idiomatic metadata format for the given {@code value}.
	 * @param value a value
	 * @return the idiomatic format for the value, or the value itself if it already
	 * complies with the idiomatic metadata format.
	 */
	public static String toDashedCase(String value) {
		StringBuilder dashed = new StringBuilder();
		Character previous = null;
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			if (SEPARATORS.contains(current)) {
				dashed.append("-");
			}
			else if (Character.isUpperCase(current) && previous != null && !SEPARATORS.contains(previous)) {
				dashed.append("-").append(current);
			}
			else {
				dashed.append(current);
			}
			previous = current;

		}
		return dashed.toString().toLowerCase(Locale.ENGLISH);
	}

}
