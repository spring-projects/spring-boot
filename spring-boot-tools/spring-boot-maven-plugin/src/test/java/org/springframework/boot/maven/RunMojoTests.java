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

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.springframework.boot.maven.RunMojo;

/**
 * Tests for {@link RunMojo}.
 * 
 * @author David Liu
 * @since 1.1.4
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RunMojo.class)
public class RunMojoTests {

	@Test
	public void testNotFork() throws Exception {
		RunMojo spy = PowerMockito.spy(new RunMojo());
		PowerMockito.doReturn("org.springframework.boot.maven.sample.ClassWithMainMethod").when(spy, "getStartClass");
		spy.execute();
	}

	@Test
	public void testFork() throws Exception {
		String[] a = { "A" };
		RunMojo spy = PowerMockito.spy(new RunMojo());
		Whitebox.setInternalState(spy, "fork", true);
		File file = new File("./target/test-classes");
		PowerMockito.doReturn(new URL[] { file.toURI().toURL() }).when(spy, "getClassPathUrls");
		Whitebox.setInternalState(spy, "arguments", a);
		PowerMockito.doReturn("org.springframework.boot.maven.sample.ClassWithMainMethod").when(spy, "getStartClass");
		spy.execute();
	}

}
