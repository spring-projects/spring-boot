/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResourceDirectory}.
 *
 * @author Andy Wilkinson
 */
class WithResourceDirectoryTests {

	@Test
	@WithResourceDirectory("test")
	void whenWithResourceDirectoryIsUsedOnAMethodThenDirectoryIsCreated() throws IOException {
		assertThat(new ClassPathResource("test").getFile()).isDirectory();
	}

	@Test
	@WithResourceDirectory("com/example/nested")
	void whenWithResourceDirectoryNamesANestedDirectoryThenDirectoryIsCreated() throws IOException {
		assertThat(new ClassPathResource("com/example/nested").getFile()).isDirectory();
	}

	@Test
	@WithResourceDirectory("1")
	@WithResourceDirectory("2")
	@WithResourceDirectory("3")
	void whenWithResourceDirectoryIsRepeatedOnAMethodThenAllResourceDirectoriesAreCreated() throws IOException {
		assertThat(new ClassPathResource("1").getFile()).isDirectory();
		assertThat(new ClassPathResource("2").getFile()).isDirectory();
		assertThat(new ClassPathResource("3").getFile()).isDirectory();
	}

}
