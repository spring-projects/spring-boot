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

package org.springframework.bootstrap.launcher;

import java.util.List;
import java.util.jar.JarEntry;

import org.springframework.bootstrap.launcher.jar.RandomAccessJarFile;

/**
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /lib} directory.
 * 
 * @author Phillip Webb
 */
public class JarLauncher extends Launcher {

	@Override
	protected boolean isNestedJarFile(JarEntry jarEntry) {
		return !jarEntry.isDirectory() && jarEntry.getName().startsWith("lib/");
	}

	@Override
	protected void postProcessLib(RandomAccessJarFile jarFile,
			List<RandomAccessJarFile> lib) throws Exception {
		lib.add(0, jarFile);
	}

	public static void main(String[] args) {
		new JarLauncher().launch(args);
	}

}
