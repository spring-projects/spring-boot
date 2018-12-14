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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.FileSystemUtils;

/**
 * Base class for all {@link ApplicationLauncher} implementations.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractApplicationLauncher implements ApplicationLauncher {

	private final Directories directories;

	AbstractApplicationLauncher(Directories directories) {
		this.directories = directories;
	}

	protected final void copyApplicationTo(File location) throws IOException {
		FileSystemUtils.deleteRecursively(location);
		location.mkdirs();
		FileSystemUtils.copyRecursively(
				new File(this.directories.getTestClassesDirectory(), "com"),
				new File(location, "com"));
	}

	protected final List<String> getDependencyJarPaths() {
		return Stream.of(this.directories.getDependenciesDirectory().listFiles())
				.map(File::getAbsolutePath).collect(Collectors.toList());
	}

	protected final Directories getDirectories() {
		return this.directories;
	}

}
