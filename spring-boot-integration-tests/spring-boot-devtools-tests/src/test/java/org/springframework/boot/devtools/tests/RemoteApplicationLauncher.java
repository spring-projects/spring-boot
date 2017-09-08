/*
 * Copyright 2012-2017 the original author or authors.
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
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.devtools.RemoteSpringApplication;
import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
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
		LaunchedJvm applicationJvm = javaLauncher.launch("app",
				createApplicationClassPath(), "com.example.DevToolsTestApplication",
				"--server.port=12345", "--spring.devtools.remote.secret=secret");
		awaitServerPort(applicationJvm.getStandardOut());
		LaunchedJvm remoteSpringApplicationJvm = javaLauncher.launch(
				"remote-spring-application", createRemoteSpringApplicationClassPath(),
				RemoteSpringApplication.class.getName(),
				"--spring.devtools.remote.secret=secret", "http://localhost:12345");
		awaitRemoteSpringApplication(remoteSpringApplicationJvm.getStandardOut());
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
		List<String> entries = new ArrayList<>();
		entries.add("target/remote");
		for (File jar : new File("target/dependencies").listFiles()) {
			entries.add(jar.getAbsolutePath());
		}
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	private int awaitServerPort(File standardOut) throws Exception {
		long end = System.currentTimeMillis() + 30000;
		File serverPortFile = new File("target/server.port");
		while (serverPortFile.length() == 0) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(String.format(
						"server.port file was not written within 30 seconds. "
								+ "Application output:%n%s",
						FileCopyUtils.copyToString(new FileReader(standardOut))));
			}
			Thread.sleep(100);
		}
		FileReader portReader = new FileReader(serverPortFile);
		int port = Integer.valueOf(FileCopyUtils.copyToString(portReader));
		return port;
	}

	private void awaitRemoteSpringApplication(File standardOut) throws Exception {
		long end = System.currentTimeMillis() + 30000;
		while (!standardOut.exists()) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(
						"Standard out file was not written " + "within 30 seconds");
			}
			Thread.sleep(100);
		}
		while (!FileCopyUtils.copyToString(new FileReader(standardOut))
				.contains("Started RemoteSpringApplication")) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(
						"RemoteSpringApplication did not start within 30 seconds");
			}
			Thread.sleep(100);
		}
	}

}
