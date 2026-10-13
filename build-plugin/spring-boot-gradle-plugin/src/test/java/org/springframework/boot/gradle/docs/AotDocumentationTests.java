/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AOT documentation.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleMultiDslExtension.class)
class AotDocumentationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void applyNativeImagePlugin() {
		assertThat(this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("9.7.0")
			.script(Examples.DIR + "aot/apply-native-image-plugin")
			.build("tasks")
			.getOutput()).contains("nativeCompile").contains("aotClasses");
	}

	@TestTemplate
	void applyAotPlugin() {
		assertThat(this.gradleBuild.script(Examples.DIR + "aot/apply-aot-plugin").build("tasks").getOutput())
			.contains("aotClasses")
			.doesNotContain("nativeCompile");
	}

}
