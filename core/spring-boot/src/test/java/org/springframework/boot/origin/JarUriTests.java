/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarUri}.
 *
 * @author Phillip Webb
 */
class JarUriTests {

	@Test
	void describeBootInfClassesUri() {
		JarUri uri = JarUri.from("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar"
				+ "!/BOOT-INF/classes!/application.properties");
		assertThat(uri).isNotNull();
		assertThat(uri.getDescription()).isEqualTo("project-0.0.1-SNAPSHOT.jar");
	}

	@Test
	void describeBootInfLibUri() {
		JarUri uri = JarUri.from("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar"
				+ "!/BOOT-INF/lib/nested.jar!/application.properties");
		assertThat(uri).isNotNull();
		assertThat(uri.getDescription()).isEqualTo("project-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/nested.jar");
	}

	@Test
	void describeRegularJar() {
		JarUri uri = JarUri
			.from("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar!/application.properties");
		assertThat(uri).isNotNull();
		assertThat(uri.getDescription()).isEqualTo("project-0.0.1-SNAPSHOT.jar");
	}

	@Test
	void getDescriptionMergedWithExisting() {
		JarUri uri = JarUri.from("jar:file:/project-0.0.1-SNAPSHOT.jar!/application.properties");
		assertThat(uri).isNotNull();
		assertThat(uri.getDescription("classpath: [application.properties]"))
			.isEqualTo("classpath: [application.properties] from project-0.0.1-SNAPSHOT.jar");
	}

}
