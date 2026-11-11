/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code UpgradeApplicator} is used to apply an {@link Upgrade}. Modifies the bom
 * configuration in the build file or a version property in {@code gradle.properties}.
 *
 * @author Andy Wilkinson
 */
class UpgradeApplicator {

	private final Path buildFile;

	private final Path gradleProperties;

	UpgradeApplicator(Path buildFile, Path gradleProperties) {
		this.buildFile = buildFile;
		this.gradleProperties = gradleProperties;
	}

	Path apply(Upgrade upgrade) throws IOException {
		String buildFileContents = Files.readString(this.buildFile);
		String toName = upgrade.to().getName();
		Matcher matcher = Pattern.compile("library\\(\"" + toName + "\", \"(.+)\"\\)").matcher(buildFileContents);
		if (!matcher.find()) {
			matcher = Pattern.compile("library\\(\"" + toName + "\"\\) \\{\\s+version\\(\"(.+)\"\\)", Pattern.MULTILINE)
				.matcher(buildFileContents);
			if (!matcher.find()) {
				throw new IllegalStateException("Failed to find definition for library '" + upgrade.to().getName()
						+ "' in bom '" + this.buildFile + "'");
			}
		}
		String version = matcher.group(1);
		if (version.startsWith("${") && version.endsWith("}")) {
			updateGradleProperties(upgrade, version);
			return this.gradleProperties;
		}
		else {
			updateBuildFile(upgrade, buildFileContents, matcher.start(1), matcher.end(1));
			return this.buildFile;
		}
	}

	private void updateGradleProperties(Upgrade upgrade, String version) throws IOException {
		String property = version.substring(2, version.length() - 1);
		String gradlePropertiesContents = Files.readString(this.gradleProperties);
		String modified = gradlePropertiesContents.replace(property + "=" + upgrade.from().getVersion(),
				property + "=" + upgrade.to().getVersion());
		overwrite(this.gradleProperties, modified);
	}

	private void updateBuildFile(Upgrade upgrade, String buildFileContents, int versionStart, int versionEnd)
			throws IOException {
		String modified = buildFileContents.substring(0, versionStart) + upgrade.to().getVersion()
				+ buildFileContents.substring(versionEnd);
		overwrite(this.buildFile, modified);
	}

	private void overwrite(Path target, String content) throws IOException {
		Files.writeString(target, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

}
