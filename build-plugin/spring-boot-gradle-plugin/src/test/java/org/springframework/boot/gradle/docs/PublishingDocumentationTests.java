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
 * Tests for the publishing documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class PublishingDocumentationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void mavenPublish() {
		assertThat(this.gradleBuild.script(Examples.DIR + "publishing/maven-publish")
			.build("publishingConfiguration")
			.getOutput()).contains("MavenPublication").contains("https://repo.example.com");
	}

}
