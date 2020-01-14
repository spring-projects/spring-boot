/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationLauncher} that launches a local application with DevTools enabled.
 *
 * @author Andy Wilkinson
 */
public class LocalApplicationLauncher extends AbstractApplicationLauncher {

	LocalApplicationLauncher(Directories directories) {
		super(directories);
	}

	@Override
	public LaunchedApplication launchApplication(JvmLauncher jvmLauncher, File serverPortFile) throws Exception {
		LaunchedJvm jvm = jvmLauncher.launch("local", createApplicationClassPath(),
				"com.example.DevToolsTestApplication", serverPortFile.getAbsolutePath(), "--server.port=0");
		return new LaunchedApplication(getDirectories().getAppDirectory(), jvm.getStandardOut(), jvm.getStandardError(),
				jvm.getProcess(), null, null);
	}

	@Override
	public LaunchedApplication launchApplication(JvmLauncher jvmLauncher, File serverPortFile, String... additionalArgs)
			throws Exception {
		List<String> args = new ArrayList<>(Arrays.asList("com.example.DevToolsTestApplication",
				serverPortFile.getAbsolutePath(), "--server.port=0"));
		args.addAll(Arrays.asList(additionalArgs));
		LaunchedJvm jvm = jvmLauncher.launch("local", createApplicationClassPath(), args.toArray(new String[] {}));
		return new LaunchedApplication(getDirectories().getAppDirectory(), jvm.getStandardOut(), jvm.getStandardError(),
				jvm.getProcess(), null, null);
	}

	protected String createApplicationClassPath() throws Exception {
		File appDirectory = getDirectories().getAppDirectory();
		copyApplicationTo(appDirectory);
		List<String> entries = new ArrayList<>();
		entries.add(appDirectory.getAbsolutePath());
		entries.addAll(getDependencyJarPaths());
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	@Override
	public String toString() {
		return "local";
	}

}
