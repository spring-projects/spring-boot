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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Define the version number of a module. A typical version is represented
 * as {@code MAJOR.MINOR.PATCH.QUALIFIER} where the qualifier can have an
 * extra version.
 * <p>
 * For example: {@code 1.2.0.RC1} is the first release candidate of 1.2.0
 * and {@code 1.5.0.M4} is the fourth milestone of 1.5.0. The special
 * {@code RELEASE} qualifier indicates a final release (a.k.a. GA)
 * <p>
 * The main purpose of parsing a version is to compare it with another
 * version, see {@link Comparable}.
 *
 * @author Stephane Nicoll
 * @since 1.2.2
 */
public class Version implements Comparable<Version> {

	private static final Pattern VERSION_PATTERN =
			Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.([^0-9]+)(\\d+)?)?$");

	private static final VersionQualifierComparator qualifierComparator = new VersionQualifierComparator();

	private final Integer major;

	private final Integer minor;

	private final Integer patch;

	private final Qualifier qualifier;

	public Version(Integer major, Integer minor, Integer patch, Qualifier qualifier) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.qualifier = qualifier;
	}

	public Integer getMajor() {
		return major;
	}

	public Integer getMinor() {
		return minor;
	}

	public Integer getPatch() {
		return patch;
	}

	public Qualifier getQualifier() {
		return qualifier;
	}

	/**
	 * Parse the string representation of a {@link Version}. Throws an
	 * {@link InvalidVersionException} if the version could not be parsed.
	 * @param text the version text
	 * @return a Version instance for the specified version text
	 * @throws InvalidVersionException if the version text could not be parsed
	 * @see #safeParse(java.lang.String)
	 */
	public static Version parse(String text) {
		Assert.notNull(text, "Text must not be null");
		Matcher matcher = VERSION_PATTERN.matcher(text.trim());
		if (!matcher.matches()) {
			throw new InvalidVersionException("Could not determine version based on '" + text + "': version format " +
					"is Minor.Major.Patch.Qualifier (i.e. 1.0.5.RELEASE");
		}
		Integer major = Integer.valueOf(matcher.group(1));
		Integer minor = Integer.valueOf(matcher.group(2));
		Integer patch = Integer.valueOf(matcher.group(3));
		Qualifier qualifier = null;
		String qualifierId = matcher.group(4);
		if (qualifierId != null) {
			String qualifierVersion = matcher.group(5);
			if (qualifierVersion != null) {
				qualifier = new Qualifier(qualifierId, Integer.valueOf(qualifierVersion));
			}
			else {
				qualifier = new Qualifier(qualifierId);
			}
		}
		return new Version(major, minor, patch, qualifier);
	}

	/**
	 * Parse safely the specified string representation of a {@link Version}.
	 * <p>
	 * Return {@code null} if the text represents an invalid version.
	 * @param text the version text
	 * @return a Version instance for the specified version text
	 * @see #parse(java.lang.String)
	 */
	public static Version safeParse(String text) {
		try {
			return parse(text);
		}
		catch (InvalidVersionException e) {
			return null;
		}
	}

	@Override
	public int compareTo(Version other) {
		if (other == null) {
			return 1;
		}
		int majorDiff = safeCompare(this.major, other.major);
		if (majorDiff != 0) {
			return majorDiff;
		}
		int minorDiff = safeCompare(this.minor, other.minor);
		if (minorDiff != 0) {
			return minorDiff;
		}
		int patch = safeCompare(this.patch, other.patch);
		if (patch != 0) {
			return patch;
		}
		return qualifierComparator.compare(this.qualifier, other.qualifier);
	}

	private static int safeCompare(Integer first, Integer second) {
		Integer firstIndex = (first != null ? first : 0);
		Integer secondIndex = (second != null ? second : 0);
		return firstIndex.compareTo(secondIndex);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Version{");
		sb.append("major=").append(major);
		sb.append(", minor=").append(minor);
		sb.append(", patch=").append(patch);
		sb.append(", qualifier=").append(qualifier);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Version version = (Version) o;

		if (major != null ? !major.equals(version.major) : version.major != null) return false;
		if (minor != null ? !minor.equals(version.minor) : version.minor != null) return false;
		if (patch != null ? !patch.equals(version.patch) : version.patch != null) return false;
		return !(qualifier != null ? !qualifier.equals(version.qualifier) : version.qualifier != null);

	}

	@Override
	public int hashCode() {
		int result = major != null ? major.hashCode() : 0;
		result = 31 * result + (minor != null ? minor.hashCode() : 0);
		result = 31 * result + (patch != null ? patch.hashCode() : 0);
		result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
		return result;
	}

	public static class Qualifier {
		private final String id;

		private final Integer version;

		public Qualifier(String id, Integer version) {
			this.id = id;
			this.version = version;
		}

		public Qualifier(String id) {
			this(id, null);
		}

		public String getId() {
			return id;
		}

		public Integer getVersion() {
			return version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Qualifier qualifier1 = (Qualifier) o;

			if (!id.equals(qualifier1.id)) return false;
			return !(version != null ? !version.equals(qualifier1.version) : qualifier1.version != null);

		}

		@Override
		public int hashCode() {
			int result = id.hashCode();
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}
	}


	private static class VersionQualifierComparator implements Comparator<Qualifier> {

		static final String RELEASE = "RELEASE";

		static final String SNAPSHOT = "BUILD-SNAPSHOT";

		static final String MILESTONE = "M";

		static final String RC = "RC";

		static final List<String> KNOWN_QUALIFIERS = Arrays.asList(MILESTONE, RC, SNAPSHOT, RELEASE);

		@Override
		public int compare(Qualifier o1, Qualifier o2) {
			Qualifier first = (o1 != null ? o1 : new Qualifier(RELEASE));
			Qualifier second = (o2 != null ? o2 : new Qualifier(RELEASE));

			int qualifier = compareQualifier(first, second);
			return (qualifier != 0 ? qualifier : compareQualifierVersion(first, second));
		}

		private static int compareQualifierVersion(Qualifier first, Qualifier second) {
			Integer firstVersion = (first.getVersion() != null ? first.getVersion() : 0);
			Integer secondVersion = (second.getVersion() != null ? second.getVersion() : 0);
			return firstVersion.compareTo(secondVersion);
		}

		private static int compareQualifier(Qualifier first, Qualifier second) {
			Integer firstIndex = getQualifierIndex(first.id);
			Integer secondIndex = getQualifierIndex(second.id);

			if (firstIndex == -1 && secondIndex == -1) { // Unknown qualifier, alphabetic ordering
				return first.id.compareTo(second.id);
			}
			else {
				return firstIndex.compareTo(secondIndex);
			}
		}

		private static int getQualifierIndex(String qualifier) {
			String q = (qualifier != null ? qualifier : RELEASE);
			return KNOWN_QUALIFIERS.indexOf(q);
		}
	}

}
