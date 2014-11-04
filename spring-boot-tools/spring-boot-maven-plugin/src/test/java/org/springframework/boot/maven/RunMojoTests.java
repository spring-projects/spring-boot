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

package org.springframework.boot.maven;

import static org.mockito.Mockito.mock;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

/**
 * Tests for {@link RunMojo}.
 *
 * @author David Liu
 * @since 1.1.4
 */
public class RunMojoTests {

	@Test
	public void testNotFork() throws Exception {
		RunMojo spy = mock(RunMojo.class);
		Whitebox.setInternalState(spy, "mainClass", "org.springframework.boot.maven.sample.ClassWithMainMethod");
		spy.execute();
	}

	@Test
	public void testFork() throws Exception {
		String[] a = { "A" };
		RunMojo spy = mock(RunMojo.class);
		Whitebox.setInternalState(spy, "fork", true);
		MavenProject project = mock(MavenProject.class);
		Whitebox.setInternalState(spy, "project", project);
		File file = new File("./target/test-classes");
		Whitebox.setInternalState(spy, "classesDirectory", file);
		// PowerMockito.doReturn(new URL[] { file.toURI().toURL() }).when(spy,
		// "getClassPathUrls");
		Whitebox.setInternalState(spy, "arguments", a);
		Whitebox.setInternalState(spy, "mainClass", "org.springframework.boot.maven.sample.ClassWithMainMethod");
		spy.execute();
	}

}
