/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class JarLauncherTests implements Runnable {

	@Rule
	public OutputCapture output = new OutputCapture();

	private ClassLoader classLoader;

	@Override
	public void run() {
		System.out.println("FOO");
	}

	@Test
	public void springFactoriesLoaded() throws Exception {
		// Slight duplication of LaunchedURLClassLoaderTests but checking that JarLauncher
		// gets the classpath right when it creates the class loader
		JarLauncher launcher = new JarLauncher() {
			@Override
			protected String getMainClass() throws Exception {
				return "org.springframework.boot.loader.JarLauncherTests";
			}

			@Override
			protected void launch(String[] args, String mainClass, ClassLoader classLoader)
					throws Exception {
				JarLauncherTests.this.classLoader = classLoader;
			}
		};
		launcher.launch(new String[0]);
		ArrayList<URL> list = Collections.list(this.classLoader
				.getResources("META-INF/spring.factories"));
		// One from src/test/resources and one from the lib/plugin.jar
		assertEquals(2, list.size());
	}

	public static void main(String[] args) {
	}

}
