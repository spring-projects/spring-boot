/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.LinkType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UpgradeBom}.
 *
 * @author Van Vo
 */
class UpgradeBomTests {

	@TempDir
	File temp;

	@ParameterizedTest
	@ValueSource(strings = { "4.0.9", "4.1.2", "4.2.1", "4.2.10" })
	void whenMaintenanceReleaseThenUpgradeReleaseNotesAreNotPrinted(String milestone) {
		UpgradeBom task = createTask(milestone);
		assertThat(captureReleaseNotes(task, List.of(upgrade(null)))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "4.2.0-M1", "4.2.0-RC1", "4.2.0", "5.0.0" })
	void whenFeatureReleaseThenUpgradeReleaseNotesArePrinted(String milestone) {
		UpgradeBom task = createTask(milestone);
		assertThat(captureReleaseNotes(task, List.of(upgrade(null)))).contains("Upgrade release notes:",
				"* Test 1.1.0");
	}

	@Test
	void whenUpgradeHasReleaseNotesLinkThenLinkIsPrinted() {
		UpgradeBom task = createTask("4.2.0-RC1");
		assertThat(captureReleaseNotes(task, List.of(upgrade("release-notes"))))
			.contains("* release-notes[Test 1.1.0]");
	}

	@Test
	void whenNoUpgradesThenUpgradeReleaseNotesAreNotPrinted() {
		UpgradeBom task = createTask("4.2.0-RC1");
		assertThat(captureReleaseNotes(task, List.of())).isEmpty();
	}

	private UpgradeBom createTask(String milestone) {
		Project project = ProjectBuilder.builder().withProjectDir(this.temp).withGradleUserHomeDir(this.temp).build();
		project.setVersion("4.2.0-SNAPSHOT");
		UpgradeBom task = project.getTasks().register("bomrUpgrade", UpgradeBom.class, mock(BomExtension.class)).get();
		task.getMilestone().set(milestone);
		return task;
	}

	private Upgrade upgrade(String releaseNotesLink) {
		Library library = mock(Library.class);
		given(library.getNameAndVersion()).willReturn("Test 1.1.0");
		given(library.getLinkUrl(LinkType.RELEASE_NOTES)).willReturn(releaseNotesLink);
		return new Upgrade(library, library);
	}

	private String captureReleaseNotes(UpgradeBom task, List<Upgrade> upgrades) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream original = System.out;
		try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(captured);
			task.upgradesApplied(upgrades);
		}
		finally {
			System.setOut(original);
		}
		return output.toString(StandardCharsets.UTF_8);
	}

}
