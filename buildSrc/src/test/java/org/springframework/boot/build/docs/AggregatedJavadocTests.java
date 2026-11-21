/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.docs;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AggregatedJavadoc}.
 *
 * @author Andy Wilkinson
 * @author Manthan Sharma
 */
class AggregatedJavadocTests {

	@Test
	void defaultJavadocListsDirectoryConvention(@TempDir File temp) {
		Project project = ProjectBuilder.builder().withProjectDir(temp).build();
		AggregatedJavadoc task = project.getTasks().register("aggregatedJavadoc", AggregatedJavadoc.class).get();
		File expected = new File(temp, "build/docs/javadoclists");
		assertThat(task.getJavadocListsDirectory().getAsFile().get()).isEqualTo(expected);
	}

	@Test
	void customJavadocListsDirectory(@TempDir File temp) {
		Project project = ProjectBuilder.builder().withProjectDir(temp).build();
		AggregatedJavadoc task = project.getTasks().register("aggregatedJavadoc", AggregatedJavadoc.class).get();
		File customDir = new File(temp, "custom/javadoclists");
		task.getJavadocListsDirectory().set(customDir);
		assertThat(task.getJavadocListsDirectory().getAsFile().get()).isEqualTo(customDir);
	}

}
