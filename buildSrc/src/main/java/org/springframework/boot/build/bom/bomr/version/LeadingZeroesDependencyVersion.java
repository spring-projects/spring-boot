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

package org.springframework.boot.build.bom.bomr.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * A {@link DependencyVersion} that tolerates leading zeroes.
 *
 * @author Andy Wilkinson
 */
final class LeadingZeroesDependencyVersion extends ArtifactVersionDependencyVersion {

	private static final Pattern PATTERN = Pattern.compile("0*([0-9]+)\\.0*([0-9]+)\\.0*([0-9]+)");

	private final String original;

	private LeadingZeroesDependencyVersion(ArtifactVersion artifactVersion, String original) {
		super(artifactVersion);
		this.original = original;
	}

	@Override
	public String toString() {
		return this.original;
	}

	static LeadingZeroesDependencyVersion parse(String input) {
		Matcher matcher = PATTERN.matcher(input);
		if (!matcher.matches()) {
			return null;
		}
		ArtifactVersion artifactVersion = new DefaultArtifactVersion(
				matcher.group(1) + matcher.group(2) + matcher.group(3));
		return new LeadingZeroesDependencyVersion(artifactVersion, input);
	}

}
