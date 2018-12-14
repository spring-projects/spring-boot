/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
public class MainClassFinderTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private TestJarFile testJarFile;

	@Before
	public void setup() throws IOException {
		this.testJarFile = new TestJarFile(this.temporaryFolder);
	}

	@Test
	public void findMainClassInJar() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarFile(), "");
		assertThat(actual).isEqualTo("B");
	}

	@Test
	public void findMainClassInJarSubFolder() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarFile(), "");
		assertThat(actual).isEqualTo("a.b.c.D");
	}

	@Test
	public void usesBreadthFirstJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarFile(), "");
		assertThat(actual).isEqualTo("a.B");
	}

	@Test
	public void findSingleJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> MainClassFinder
						.findSingleMainClass(this.testJarFile.getJarFile(), ""))
				.withMessageContaining("Unable to find a single main class "
						+ "from the following candidates [a.B, a.b.c.E]");
	}

	@Test
	public void findSingleJarSearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		String mainClass = MainClassFinder.findSingleMainClass(
				this.testJarFile.getJarFile(), "",
				"org.springframework.boot.loader.tools.sample.SomeApplication");
		assertThat(mainClass).isEqualTo("a.b.c.E");
	}

	@Test
	public void findMainClassInJarSubLocation() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarFile(),
				"a/");
		assertThat(actual).isEqualTo("B");

	}

	@Test
	public void findMainClassInFolder() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("B");
	}

	@Test
	public void findMainClassInSubFolder() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.b.c.D");
	}

	@Test
	public void usesBreadthFirstFolderSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.B");
	}

	@Test
	public void findSingleFolderSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> MainClassFinder
						.findSingleMainClass(this.testJarFile.getJarSource()))
				.withMessageContaining("Unable to find a single main class "
						+ "from the following candidates [a.B, a.b.c.E]");
	}

	@Test
	public void findSingleFolderSearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		String mainClass = MainClassFinder.findSingleMainClass(
				this.testJarFile.getJarSource(),
				"org.springframework.boot.loader.tools.sample.SomeApplication");
		assertThat(mainClass).isEqualTo("a.b.c.E");
	}

	@Test
	public void doWithFolderMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarSource(), callback);
		assertThat(callback.getClassNames().toString()).isEqualTo("[a.b.G, a.b.c.D]");
	}

	@Test
	public void doWithJarMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarFile(), null, callback);
		assertThat(callback.getClassNames().toString()).isEqualTo("[a.b.G, a.b.c.D]");
	}

	private static class ClassNameCollector implements MainClassCallback<Object> {

		private final List<String> classNames = new ArrayList<>();

		@Override
		public Object doWith(MainClass mainClass) {
			this.classNames.add(mainClass.getName());
			return null;
		}

		public List<String> getClassNames() {
			return this.classNames;
		}

	}

}
