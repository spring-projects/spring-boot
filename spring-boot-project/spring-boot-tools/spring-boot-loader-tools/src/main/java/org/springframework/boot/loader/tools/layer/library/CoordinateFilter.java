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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

	private final List<String> includes = new ArrayList<>();

	private final List<String> excludes = new ArrayList<>();

	public CoordinateFilter(List<String> includes, List<String> excludes) {
		this.includes.addAll(includes);
		this.excludes.addAll(excludes);
	}

	@Override
	public boolean isLibraryIncluded(Library library) {
		return isMatch(library, this.includes);
	}

	@Override
	public boolean isLibraryExcluded(Library library) {
		return isMatch(library, this.excludes);
	}

	private boolean isMatch(Library library, List<String> toMatch) {
		StringBuilder builder = new StringBuilder();
		LibraryCoordinates coordinates = library.getCoordinates();
		if (coordinates != null) {
			if (coordinates.getGroupId() != null) {
				builder.append(coordinates.getGroupId());
			}
			builder.append(":");
			if (coordinates.getArtifactId() != null) {
				builder.append(coordinates.getArtifactId());
			}
			builder.append(":");
			if (coordinates.getVersion() != null) {
				builder.append(coordinates.getVersion());
			}
		}
		else {
			builder.append("::");
		}
		String input = builder.toString();
		for (String patternString : toMatch) {
			Pattern pattern = buildPatternForString(patternString);
			if (pattern.matcher(input).matches()) {
				return true;
			}
		}
		return false;
	}

	private Pattern buildPatternForString(String pattern) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
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

}
