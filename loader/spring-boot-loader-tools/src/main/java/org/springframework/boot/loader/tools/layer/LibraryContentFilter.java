/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools.layer;

import java.util.regex.Pattern;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;
import org.springframework.util.Assert;

/**
 * {@link ContentFilter} that matches {@link Library} items based on a coordinates
 * pattern.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.3.0
 */
public class LibraryContentFilter implements ContentFilter<Library> {

	private final Pattern pattern;

	public LibraryContentFilter(String coordinatesPattern) {
		Assert.hasText(coordinatesPattern, "'coordinatesPattern' must not be empty");
		StringBuilder regex = new StringBuilder();
		for (int i = 0; i < coordinatesPattern.length(); i++) {
			char c = coordinatesPattern.charAt(i);
			if (c == '.') {
				regex.append("\\.");
			}
			else if (c == '*') {
				regex.append(".*");
			}
			else {
				regex.append(c);
			}
		}
		this.pattern = Pattern.compile(regex.toString());
	}

	@Override
	public boolean matches(Library library) {
		return this.pattern.matcher(LibraryCoordinates.toStandardNotationString(library.getCoordinates())).matches();
	}

}
