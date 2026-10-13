/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.version;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * A fallback {@link DependencyVersion} to handle versions with four or five components
 * that cannot be handled by {@link ArtifactVersion} because the fourth component is
 * numeric.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
final class MultipleComponentsDependencyVersion extends ArtifactVersionDependencyVersion {

	private final String original;

	private MultipleComponentsDependencyVersion(ArtifactVersion artifactVersion, String original) {
		super(artifactVersion, new ComparableVersion(original));
		this.original = original;
	}

	@Override
	public String toString() {
		return this.original;
	}

	static MultipleComponentsDependencyVersion parse(String input) {
		String[] components = input.split("\\.");
		if (components.length == 4 || components.length == 5) {
			ArtifactVersion artifactVersion = new DefaultArtifactVersion(
					components[0] + "." + components[1] + "." + components[2]);
			if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(input)) {
				return null;
			}
			return new MultipleComponentsDependencyVersion(artifactVersion, input);
		}
		return null;
	}

}
