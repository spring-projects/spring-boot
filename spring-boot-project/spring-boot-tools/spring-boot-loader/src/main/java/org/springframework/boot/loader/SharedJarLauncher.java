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

package org.springframework.boot.loader;

import java.util.List;

import org.springframework.boot.loader.archive.Archive;

/**
 * {@link SharedJarLauncher} for JAR based archives. This launcher assumes that dependency
 * jars are included inside a {@code /BOOT-INF/lib} directory and that application classes
 * are included inside a {@code /BOOT-INF/classes} directory.
 * <p>
 * If called multiple times the launcher will reuse the previous classloader and call the
 * same instance again. This is used to allow multiple calls to the same booting
 * application to allow commands like "stop" or "reload"
 *
 * @author Robert Alexandersson
 * @since 2.2.0
 */
public class SharedJarLauncher extends JarLauncher {

	protected static SharedJarLauncher INSTANCE;

	protected ClassLoader classLoader;

	public static SharedJarLauncher getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SharedJarLauncher();
		}
		return INSTANCE;
	}

	@Override
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		if (this.classLoader != null) {
			return this.classLoader;
		}

		this.classLoader = super.createClassLoader(archives);

		return this.classLoader;
	}

	public static void main(String[] args) throws Exception {
		getInstance().launch(args);
	}

}
