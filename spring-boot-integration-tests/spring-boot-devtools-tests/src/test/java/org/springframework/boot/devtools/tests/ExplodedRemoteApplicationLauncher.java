/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationLauncher} that launches a remote application with its classes
 * available directly on the file system.
 *
 * @author Andy Wilkinson
 */
public class ExplodedRemoteApplicationLauncher extends RemoteApplicationLauncher {

	@Override
	protected String createApplicationClassPath() throws Exception {
		File appDirectory = new File("target/app");
		FileSystemUtils.deleteRecursively(appDirectory);
		appDirectory.mkdirs();
		FileSystemUtils.copyRecursively(new File("target/test-classes/com"),
				new File("target/app/com"));
		List<String> entries = new ArrayList<String>();
		entries.add("target/app");
		for (File jar : new File("target/dependencies").listFiles()) {
			entries.add(jar.getAbsolutePath());
		}
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	@Override
	public String toString() {
		return "exploded remote";
	}

}
