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
 * A {@link DependencyVersion} where the patch and qualifier are not separated.
 *
 * @author Andy Wilkinson
 */
final class CombinedPatchAndQualifierDependencyVersion extends ArtifactVersionDependencyVersion {

	private static final Pattern PATTERN = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)([A-Za-z][A-Za-z0-9]+)");

	private final String original;

	/**
	 * Constructs a new CombinedPatchAndQualifierDependencyVersion object with the
	 * specified artifact version and original string.
	 * @param artifactVersion the artifact version to be used
	 * @param original the original string to be used
	 */
	private CombinedPatchAndQualifierDependencyVersion(ArtifactVersion artifactVersion, String original) {
		super(artifactVersion);
		this.original = original;
	}

	/**
	 * Returns the string representation of the CombinedPatchAndQualifierDependencyVersion
	 * object.
	 * @return the original string value of the CombinedPatchAndQualifierDependencyVersion
	 * object
	 */
	@Override
	public String toString() {
		return this.original;
	}

	/**
	 * Parses the given version string and returns a
	 * CombinedPatchAndQualifierDependencyVersion object.
	 * @param version the version string to parse
	 * @return a CombinedPatchAndQualifierDependencyVersion object if the version string
	 * is valid, null otherwise
	 */
	static CombinedPatchAndQualifierDependencyVersion parse(String version) {
		Matcher matcher = PATTERN.matcher(version);
		if (!matcher.matches()) {
			return null;
		}
		ArtifactVersion artifactVersion = new DefaultArtifactVersion(matcher.group(1) + "." + matcher.group(2));
		if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
			return null;
		}
		return new CombinedPatchAndQualifierDependencyVersion(artifactVersion, version);
	}

}
