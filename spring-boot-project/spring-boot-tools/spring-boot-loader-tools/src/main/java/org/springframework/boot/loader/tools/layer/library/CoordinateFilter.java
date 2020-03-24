/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools.layer.library;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * An implementation of {@link LibraryFilter} based on the library's coordinates.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 2.3.0
 */
public class CoordinateFilter implements LibraryFilter {

	private static final String EMPTY_COORDINATES = "::";

	private final List<Pattern> includes;

	private final List<Pattern> excludes;

	public CoordinateFilter(List<String> includes, List<String> excludes) {
		this.includes = includes.stream().map(this::asPattern).collect(Collectors.toList());
		this.excludes = excludes.stream().map(this::asPattern).collect(Collectors.toList());
	}

	private Pattern asPattern(String string) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '.') {
				builder.append("\\.");
			}
			else if (c == '*') {
				builder.append(".*");
			}
			else {
				builder.append(c);
			}
		}
		return Pattern.compile(builder.toString());
	}

	@Override
	public boolean isLibraryIncluded(Library library) {
		return isMatch(library, this.includes);
	}

	@Override
	public boolean isLibraryExcluded(Library library) {
		return isMatch(library, this.excludes);
	}

	private boolean isMatch(Library library, List<Pattern> patterns) {
		LibraryCoordinates coordinates = library.getCoordinates();
		String input = (coordinates != null) ? coordinates.toString() : EMPTY_COORDINATES;
		for (Pattern pattern : patterns) {
			if (pattern.matcher(input).matches()) {
				return true;
			}
		}
		return false;
	}

}
