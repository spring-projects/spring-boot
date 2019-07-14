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

package org.springframework.boot.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.archive.JarCommand;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.boot.cli.command.run.RunCommand;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

/**
 * JUnit 5 {@link Extension} that can be used to invoke CLI commands.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class CliTester implements BeforeEachCallback, AfterEachCallback {

	private final File temp;

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private final CapturedOutput output;

	private String previousOutput = "";

	private long timeout = TimeUnit.MINUTES.toMillis(6);

	private final List<AbstractCommand> commands = new ArrayList<>();

	private final String prefix;

	private File serverPortFile;

	public CliTester(String prefix, CapturedOutput output) {
		this.prefix = prefix;
		try {
			this.temp = Files.createTempDirectory("cli-tester").toFile();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create temp directory");
		}
		this.output = output;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public String run(String... args) throws Exception {
		List<String> updatedArgs = new ArrayList<>();
		boolean classpathUpdated = false;
		for (String arg : args) {
			if (arg.startsWith("--classpath=")) {
				arg = arg + ":" + this.buildOutput.getTestClassesLocation().getAbsolutePath();
				classpathUpdated = true;
			}
			updatedArgs.add(arg);
		}
		if (!classpathUpdated) {
			updatedArgs.add("--classpath=.:" + this.buildOutput.getTestClassesLocation().getAbsolutePath());
		}
		Future<RunCommand> future = submitCommand(new RunCommand(), StringUtils.toStringArray(updatedArgs));
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	public String grab(String... args) throws Exception {
		Future<GrabCommand> future = submitCommand(new GrabCommand(), args);
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	public String jar(String... args) throws Exception {
		Future<JarCommand> future = submitCommand(new JarCommand(), args);
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	public File getTemp() {
		return this.temp;
	}

	private <T extends OptionParsingCommand> Future<T> submitCommand(T command, String... args) {
		clearUrlHandler();
		final String[] sources = getSources(args);
		return Executors.newSingleThreadExecutor().submit(() -> {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			System.setProperty("server.port", "0");
			System.setProperty("spring.application.class.name",
					"org.springframework.boot.cli.CliTesterSpringApplication");
			this.serverPortFile = new File(this.temp, "server.port");
			System.setProperty("portfile", this.serverPortFile.getAbsolutePath());
			try {
				command.run(sources);
				return command;
			}
			finally {
				System.clearProperty("server.port");
				System.clearProperty("spring.application.class.name");
				System.clearProperty("portfile");
				Thread.currentThread().setContextClassLoader(loader);
			}
		});
	}

	/**
	 * The TomcatURLStreamHandlerFactory fails if the factory is already set, use
	 * reflection to reset it.
	 */
	private void clearUrlHandler() {
		try {
			Field field = URL.class.getDeclaredField("factory");
			field.setAccessible(true);
			field.set(null, null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected String[] getSources(String... args) {
		final String[] sources = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (!arg.endsWith(".groovy") && !arg.endsWith(".xml")) {
				if (new File(this.prefix + arg).isDirectory()) {
					sources[i] = this.prefix + arg;
				}
				else {
					sources[i] = arg;
				}
			}
			else {
				sources[i] = new File(arg).isAbsolute() ? arg : this.prefix + arg;
			}
		}
		return sources;
	}

	private String getOutput() {
		String output = this.output.toString().substring(this.previousOutput.length());
		this.previousOutput = output;
		return output;
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) {
		Assumptions.assumeTrue(System.getProperty("spring.profiles.active", "integration").contains("integration"),
				"Not running sample integration tests because integration profile not active");
		System.setProperty("disableSpringSnapshotRepos", "false");
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) {
		for (AbstractCommand command : this.commands) {
			if (command != null && command instanceof RunCommand) {
				((RunCommand) command).stop();
			}
		}
		System.clearProperty("disableSpringSnapshotRepos");
		FileSystemUtils.deleteRecursively(this.temp);
	}

	public String getHttpOutput() {
		return getHttpOutput("/");
	}

	public String getHttpOutput(String uri) {
		try {
			int port = Integer.parseInt(FileCopyUtils.copyToString(new FileReader(this.serverPortFile)));
			InputStream stream = URI.create("http://localhost:" + port + uri).toURL().openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			return reader.lines().collect(Collectors.joining());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
