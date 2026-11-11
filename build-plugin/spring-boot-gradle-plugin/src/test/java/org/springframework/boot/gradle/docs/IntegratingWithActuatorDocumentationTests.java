/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.docs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the generating build info documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class IntegratingWithActuatorDocumentationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void basicBuildInfo() {
		this.gradleBuild.script(Examples.DIR + "integrating-with-actuator/build-info-basic").build("bootBuildInfo");
		assertThat(new File(this.gradleBuild.getProjectDir(), "build/bootBuildInfo/META-INF/build-info.properties"))
			.isFile();
	}

	@TestTemplate
	void buildInfoCustomValues() {
		this.gradleBuild.script(Examples.DIR + "integrating-with-actuator/build-info-custom-values")
			.build("bootBuildInfo");
		File file = new File(this.gradleBuild.getProjectDir(), "build/bootBuildInfo/META-INF/build-info.properties");
		assertThat(file).isFile();
		Properties properties = buildInfoProperties(file);
		assertThat(properties).containsEntry("build.artifact", "example-app");
		assertThat(properties).containsEntry("build.version", "1.2.3");
		assertThat(properties).containsEntry("build.group", "com.example");
		assertThat(properties).containsEntry("build.name", "Example application");
		assertThat(properties).containsKey("build.time");
	}

	@TestTemplate
	void buildInfoAdditional() {
		this.gradleBuild.script(Examples.DIR + "integrating-with-actuator/build-info-additional")
			.build("bootBuildInfo");
		File file = new File(this.gradleBuild.getProjectDir(), "build/bootBuildInfo/META-INF/build-info.properties");
		assertThat(file).isFile();
		Properties properties = buildInfoProperties(file);
		assertThat(properties).containsEntry("build.a", "alpha");
		assertThat(properties).containsEntry("build.b", "bravo");
	}

	@TestTemplate
	void buildInfoExcludeTime() {
		this.gradleBuild.script(Examples.DIR + "integrating-with-actuator/build-info-exclude-time")
			.build("bootBuildInfo");
		File file = new File(this.gradleBuild.getProjectDir(), "build/bootBuildInfo/META-INF/build-info.properties");
		assertThat(file).isFile();
		Properties properties = buildInfoProperties(file);
		assertThat(properties).doesNotContainKey("build.time");
	}

	private Properties buildInfoProperties(File file) {
		assertThat(file).isFile();
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(file)) {
			properties.load(reader);
			return properties;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
