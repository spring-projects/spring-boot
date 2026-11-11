/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

/**
 * Tests for the getting started documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class GettingStartedDocumentationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	// NOTE: We can't run any 'apply-plugin' tests because during a release the
	// jar won't be there

	@TestTemplate
	void typicalPluginsAppliesExceptedPlugins() {
		this.gradleBuild.script(Examples.DIR + "getting-started/typical-plugins").build("verify");
	}

}
