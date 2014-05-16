/*
 * Copyright 2012-2014 the original author or authors.
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
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.loader.tools.MainClassFinder.ClassNameCallback;
import org.springframework.boot.loader.tools.sample.ClassWithMainMethod;
import org.springframework.boot.loader.tools.sample.ClassWithoutMainMethod;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link MainClassFinder}.
 * 
 * @author Phillip Webb
 */
public class MainClassFinderTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
		assertThat(actual, equalTo("B"));
	}

	@Test
	public void findMainClassInJarSubFolder() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarFile(), "");
		assertThat(actual, equalTo("a.b.c.D"));
	}

	@Test
	public void usesBreadthFirstJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarFile(), "");
		assertThat(actual, equalTo("a.B"));
	}

	@Test
	public void findSingleJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to find a single main class "
				+ "from the following candidates [a.B, a.b.c.E]");
		MainClassFinder.findSingleMainClass(this.testJarFile.getJarFile(), "");
	}

	@Test
	public void findMainClassInJarSubLocation() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder
				.findMainClass(this.testJarFile.getJarFile(), "a/");
		assertThat(actual, equalTo("B"));

	}

	@Test
	public void findMainClassInFolder() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual, equalTo("B"));
	}

	@Test
	public void findMainClassInSubFolder() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual, equalTo("a.b.c.D"));
	}

	@Test
	public void usesBreadthFirstFolderSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual, equalTo("a.B"));
	}

	@Test
	public void findSingleFolderSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to find a single main class "
				+ "from the following candidates [a.B, a.b.c.E]");
		MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource());
	}

	@Test
	public void doWithFolderMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarSource(), callback);
		assertThat(callback.getClassNames().toString(), equalTo("[a.b.G, a.b.c.D]"));
	}

	@Test
	public void doWithJarMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarFile(), "", callback);
		assertThat(callback.getClassNames().toString(), equalTo("[a.b.G, a.b.c.D]"));
	}

	private static class ClassNameCollector implements ClassNameCallback<Object> {

		private final List<String> classNames = new ArrayList<String>();

		@Override
		public Object doWith(String className) {
			this.classNames.add(className);
			return null;
		}

		public List<String> getClassNames() {
			return this.classNames;
		}

	}

}
