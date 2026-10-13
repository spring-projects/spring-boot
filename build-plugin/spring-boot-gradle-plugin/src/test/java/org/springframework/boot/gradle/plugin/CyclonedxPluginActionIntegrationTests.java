/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CyclonedxPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class CyclonedxPluginActionIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void sbomIsIncludedInUberJar() throws IOException {
		sbomIsIncludedInUberArchive("bootJar", "");
	}

	@TestTemplate
	void sbomIsIncludedInUberWar() throws IOException {
		sbomIsIncludedInUberArchive("bootWar", "WEB-INF/classes/");
	}

	private void sbomIsIncludedInUberArchive(String taskName, String sbomLocationPrefix) throws IOException {
		BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("7.6.6").build(taskName);
		BuildTask task = result.task(":cyclonedxBom");
		assertThat(task).isNotNull().extracting(BuildTask::getOutcome).isEqualTo(TaskOutcome.SUCCESS);
		File[] libs = new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles();
		assertThat(libs).hasSize(1);
		try (JarFile jar = new JarFile(libs[0])) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Sbom-Format")).isEqualTo("CycloneDX");
			String sbomLocation = jar.getManifest().getMainAttributes().getValue("Sbom-Location");
			assertThat(sbomLocation).isEqualTo(sbomLocationPrefix + "META-INF/sbom/application.cdx.json");
			List<String> entryNames = jar.stream().map(JarEntry::getName).toList();
			assertThat(entryNames).contains(sbomLocation);
		}
	}

}
