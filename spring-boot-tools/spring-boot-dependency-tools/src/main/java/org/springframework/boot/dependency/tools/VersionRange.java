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

package org.springframework.boot.dependency.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Define a {@link Version} range.  A square bracket "[" or "]" denotes an inclusive
 * end of the range and a round bracket "(" or ")" denotes an exclusive end of the
 * range. A range can also be unbounded by defining a single {@link Version}. The
 * examples below make this hopefully more clear.
 * <ul>
 * <li>"[1.2.0.RELEASE,1.3.0.RELEASE)" version 1.2.0 and any version after
 * this, up to, but not including, version 1.3.0.</li>
 * <li>"(2.0.0.RELEASE,3.2.0.RELEASE]" any version after 2.0.0 up to and
 * including version 3.2.0.</li>
 * <li>"1.4.5.RELEASE", version 1.4.5 and all later versions.</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @since 1.2.2
 */
public class VersionRange {

	private static final Pattern RANGE_PATTERN = Pattern.compile("(\\(|\\[)(.*),(.*)(\\)|\\])");

	private final Version lowerVersion;
	private final boolean lowerInclusive;
	private final Version higherVersion;
	private final boolean higherInclusive;

	public VersionRange(Version lowerVersion, boolean lowerInclusive,
			Version higherVersion, boolean higherInclusive) {
		this.lowerVersion = lowerVersion;
		this.lowerInclusive = lowerInclusive;
		this.higherVersion = higherVersion;
		this.higherInclusive = higherInclusive;
	}

	/**
	 * Specify if the {@link Version} matches this range. Returns {@code true}
	 * if the version is contained within this range, {@code false} otherwise.
	 * @param version the version to match this range
	 * @return {@code true} if this version is contained within this range
	 */
	public boolean match(Version version) {
		Assert.notNull(version, "Version must not be null");
		int lower = lowerVersion.compareTo(version);
		if (lower > 0) {
			return false;
		} else if (!lowerInclusive && lower == 0) {
			return false;
		}
		if (higherVersion != null) {
			int higher = higherVersion.compareTo(version);
			if (higher < 0) {
				return false;
			} else if (!higherInclusive && higher == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Parse the string representation of a {@link VersionRange}. Throws an
	 * {@link InvalidVersionException} if the range could not be parsed.
	 * @param text the range text
	 * @return a VersionRange instance for the specified range text
	 * @throws InvalidVersionException if the range text could not be parsed
	 */
	public static VersionRange parse(String text) {
		Assert.notNull(text, "Text must not be null");
		Matcher matcher = RANGE_PATTERN.matcher(text.trim());
		if (!matcher.matches()) {
			// Try to read it as simple version
			Version version = Version.parse(text);
			return new VersionRange(version, true, null, true);
		}
		boolean lowerInclusive = matcher.group(1).equals("[");
		Version lowerVersion = Version.parse(matcher.group(2));
		Version higherVersion = Version.parse(matcher.group(3));
		boolean higherInclusive = matcher.group(4).equals("]");
		return new VersionRange(lowerVersion, lowerInclusive, higherVersion, higherInclusive);
	}

}
