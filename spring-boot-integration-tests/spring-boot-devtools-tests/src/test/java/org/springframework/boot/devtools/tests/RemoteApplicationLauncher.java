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

import org.springframework.boot.devtools.RemoteSpringApplication;
import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link ApplicationLauncher} implementations that use
 * {@link RemoteSpringApplication}.
 *
 * @author Andy Wilkinson
 */
abstract class RemoteApplicationLauncher implements ApplicationLauncher {

	@Override
	public LaunchedApplication launchApplication(JvmLauncher javaLauncher)
			throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		LaunchedJvm applicationJvm = javaLauncher.launch("app",
				createApplicationClassPath(), "com.example.DevToolsTestApplication",
				"--server.port=" + port, "--spring.devtools.remote.secret=secret");
		LaunchedJvm remoteSpringApplicationJvm = javaLauncher.launch(
				"remote-spring-application", createRemoteSpringApplicationClassPath(),
				RemoteSpringApplication.class.getName(),
				"--spring.devtools.remote.secret=secret", "http://localhost:" + port);
		return new LaunchedApplication(new File("target/remote"),
				applicationJvm.getStandardOut(), applicationJvm.getProcess(),
				remoteSpringApplicationJvm.getProcess());
	}

	protected abstract String createApplicationClassPath() throws Exception;

	private String createRemoteSpringApplicationClassPath() throws Exception {
		File remoteDirectory = new File("target/remote");
		FileSystemUtils.deleteRecursively(remoteDirectory);
		remoteDirectory.mkdirs();
		FileSystemUtils.copyRecursively(new File("target/test-classes/com"),
				new File("target/remote/com"));
		List<String> entries = new ArrayList<String>();
		entries.add("target/remote");
		for (File jar : new File("target/dependencies").listFiles()) {
			entries.add(jar.getAbsolutePath());
		}
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

}
