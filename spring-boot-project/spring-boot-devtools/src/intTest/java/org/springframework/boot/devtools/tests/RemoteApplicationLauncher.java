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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import org.springframework.boot.devtools.RemoteSpringApplication;
import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.containsString;

/**
 * Base class for {@link ApplicationLauncher} implementations that use
 * {@link RemoteSpringApplication}.
 *
 * @author Andy Wilkinson
 */
abstract class RemoteApplicationLauncher extends AbstractApplicationLauncher {

	RemoteApplicationLauncher(Directories directories) {
		super(directories);
	}

	@Override
	public LaunchedApplication launchApplication(JvmLauncher javaLauncher, File serverPortFile) throws Exception {
		LaunchedJvm applicationJvm = javaLauncher.launch("app", createApplicationClassPath(),
				"com.example.DevToolsTestApplication", serverPortFile.getAbsolutePath(), "--server.port=0",
				"--spring.devtools.remote.secret=secret");
		int port = awaitServerPort(applicationJvm, serverPortFile);
		BiFunction<Integer, File, Process> remoteRestarter = getRemoteRestarter(javaLauncher);
		return new LaunchedApplication(getDirectories().getRemoteAppDirectory(), applicationJvm.getStandardOut(),
				applicationJvm.getStandardError(), applicationJvm.getProcess(), remoteRestarter.apply(port, null),
				remoteRestarter);
	}

	@Override
	public LaunchedApplication launchApplication(JvmLauncher javaLauncher, File serverPortFile,
			String... additionalArgs) throws Exception {
		List<String> args = new ArrayList<>(Arrays.asList("com.example.DevToolsTestApplication",
				serverPortFile.getAbsolutePath(), "--server.port=0", "--spring.devtools.remote.secret=secret"));
		args.addAll(Arrays.asList(additionalArgs));
		LaunchedJvm applicationJvm = javaLauncher.launch("app", createApplicationClassPath(),
				args.toArray(new String[] {}));
		int port = awaitServerPort(applicationJvm, serverPortFile);
		BiFunction<Integer, File, Process> remoteRestarter = getRemoteRestarter(javaLauncher);
		return new LaunchedApplication(getDirectories().getRemoteAppDirectory(), applicationJvm.getStandardOut(),
				applicationJvm.getStandardError(), applicationJvm.getProcess(), remoteRestarter.apply(port, null),
				remoteRestarter);
	}

	private BiFunction<Integer, File, Process> getRemoteRestarter(JvmLauncher javaLauncher) {
		return (port, classesDirectory) -> {
			try {
				LaunchedJvm remoteSpringApplicationJvm = javaLauncher.launch("remote-spring-application",
						createRemoteSpringApplicationClassPath(classesDirectory),
						RemoteSpringApplication.class.getName(), "--spring.devtools.remote.secret=secret",
						"http://localhost:" + port);
				awaitRemoteSpringApplication(remoteSpringApplicationJvm);
				return remoteSpringApplicationJvm.getProcess();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		};
	}

	protected abstract String createApplicationClassPath() throws Exception;

	private String createRemoteSpringApplicationClassPath(File classesDirectory) throws Exception {
		File remoteAppDirectory = getDirectories().getRemoteAppDirectory();
		if (classesDirectory == null) {
			copyApplicationTo(remoteAppDirectory);
		}
		List<String> entries = new ArrayList<>();
		entries.add(remoteAppDirectory.getAbsolutePath());
		entries.addAll(getDependencyJarPaths());
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	private int awaitServerPort(LaunchedJvm jvm, File serverPortFile) throws Exception {
		return Awaitility.waitAtMost(Duration.ofSeconds(60))
				.until(() -> new ApplicationState(serverPortFile, jvm), ApplicationState::hasServerPort)
				.getServerPort();
	}

	private void awaitRemoteSpringApplication(LaunchedJvm launchedJvm) throws Exception {
		FileContents contents = new FileContents(launchedJvm.getStandardOut());
		try {
			Awaitility.waitAtMost(Duration.ofSeconds(30)).until(contents::get,
					containsString("Started RemoteSpringApplication"));
		}
		catch (ConditionTimeoutException ex) {
			if (!launchedJvm.getProcess().isAlive()) {
				throw new IllegalStateException(
						"Process exited with status " + launchedJvm.getProcess().exitValue()
								+ " before producing expected standard output.\n\nStandard output:\n\n" + contents.get()
								+ "\n\nStandard error:\n\n" + new FileContents(launchedJvm.getStandardError()).get(),
						ex);
			}
			throw ex;
		}
	}

}
