/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.tools.MainClassFinder.MainClass;
import org.springframework.boot.loader.tools.MainClassFinder.MainClassCallback;
import org.springframework.boot.loader.tools.sample.AnnotatedClassWithMainMethod;
import org.springframework.boot.loader.tools.sample.ClassWithMainMethod;
import org.springframework.boot.loader.tools.sample.ClassWithoutMainMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MainClassFinder}.
 *
 * @author Phillip Webb
 */
class MainClassFinderTests {

	private TestJarFile testJarFile;

	@BeforeEach
	void setup(@TempDir File tempDir) throws IOException {
		this.testJarFile = new TestJarFile(tempDir);
	}

	@Test
	void findMainClassInJar() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("B");
		}
	}

	@Test
	void findMainClassInJarSubFolder() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("a.b.c.D");
		}
	}

	@Test
	void usesBreadthFirstJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("a.B");
		}
	}

	@Test
	void findSingleJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			assertThatIllegalStateException().isThrownBy(() -> MainClassFinder.findSingleMainClass(jarFile, ""))
					.withMessageContaining(
							"Unable to find a single main class from the following candidates [a.B, a.b.c.E]");
		}
	}

	@Test
	void findSingleJarSearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String mainClass = MainClassFinder.findSingleMainClass(jarFile, "",
					"org.springframework.boot.loader.tools.sample.SomeApplication");
			assertThat(mainClass).isEqualTo("a.b.c.E");
		}
	}

	@Test
	void findMainClassInJarSubLocation() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "a/");
			assertThat(actual).isEqualTo("B");
		}

	}

	@Test
	void findMainClassInFolder() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("B");
	}

	@Test
	void findMainClassInSubFolder() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.b.c.D");
	}

	@Test
	void usesBreadthFirstFolderSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.B");
	}

	@Test
	void findSingleFolderSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource()))
				.withMessageContaining(
						"Unable to find a single main class from the following candidates [a.B, a.b.c.E]");
	}

	@Test
	void findSingleFolderSearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		String mainClass = MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource(),
				"org.springframework.boot.loader.tools.sample.SomeApplication");
		assertThat(mainClass).isEqualTo("a.b.c.E");
	}

	@Test
	void doWithFolderMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarSource(), callback);
		assertThat(callback.getClassNames().toString()).isEqualTo("[a.b.G, a.b.c.D]");
	}

	@Test
	void doWithJarMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames().toString()).isEqualTo("[a.b.G, a.b.c.D]");
		}
	}

	static class ClassNameCollector implements MainClassCallback<Object> {

		private final List<String> classNames = new ArrayList<>();

		@Override
		public Object doWith(MainClass mainClass) {
			this.classNames.add(mainClass.getName());
			return null;
		}

		List<String> getClassNames() {
			return this.classNames;
		}

	}

}
