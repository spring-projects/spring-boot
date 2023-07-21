/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.build.bom.bomr.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * A {@link DependencyVersion} for a release train such as Spring Data.
 *
 * @author Andy Wilkinson
 */
final class ReleaseTrainDependencyVersion implements DependencyVersion {

	private static final Pattern VERSION_PATTERN = Pattern.compile("([A-Z][a-z]+)-([A-Z]+)([0-9]*)");

	private final String releaseTrain;

	private final String type;

	private final int version;

	private final String original;

	private ReleaseTrainDependencyVersion(String releaseTrain, String type, int version, String original) {
		this.releaseTrain = releaseTrain;
		this.type = type;
		this.version = version;
		this.original = original;
	}

	@Override
	public int compareTo(DependencyVersion other) {
		if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
			return -1;
		}
		int comparison = this.releaseTrain.compareTo(otherReleaseTrain.releaseTrain);
		if (comparison != 0) {
			return comparison;
		}
		comparison = this.type.compareTo(otherReleaseTrain.type);
		if (comparison != 0) {
			return comparison;
		}
		return Integer.compare(this.version, otherReleaseTrain.version);
	}

	@Override
	public boolean isNewerThan(DependencyVersion other) {
		if (other instanceof CalendarVersionDependencyVersion) {
			return false;
		}
		if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
			return true;
		}
		return otherReleaseTrain.compareTo(this) < 0;
	}

	@Override
	public boolean isSameMajorAndNewerThan(DependencyVersion other) {
		return isNewerThan(other);
	}

	@Override
	public boolean isSameMinorAndNewerThan(DependencyVersion other) {
		if (other instanceof CalendarVersionDependencyVersion) {
			return false;
		}
		if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
			return true;
		}
		return otherReleaseTrain.releaseTrain.equals(this.releaseTrain) && isNewerThan(other);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ReleaseTrainDependencyVersion other = (ReleaseTrainDependencyVersion) obj;
		if (!this.original.equals(other.original)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.original.hashCode();
	}

	@Override
	public String toString() {
		return this.original;
	}

	static ReleaseTrainDependencyVersion parse(String input) {
		Matcher matcher = VERSION_PATTERN.matcher(input);
		if (!matcher.matches()) {
			return null;
		}
		return new ReleaseTrainDependencyVersion(matcher.group(1), matcher.group(2),
				(StringUtils.hasLength(matcher.group(3))) ? Integer.parseInt(matcher.group(3)) : 0, input);
	}

}
