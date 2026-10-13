/*
 * Copyright 2012-present the original author or authors.
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

	private CombinedPatchAndQualifierDependencyVersion(ArtifactVersion artifactVersion, String original) {
		super(artifactVersion);
		this.original = original;
	}

	@Override
	public String toString() {
		return this.original;
	}

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
