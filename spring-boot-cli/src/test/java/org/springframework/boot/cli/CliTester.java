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

package org.springframework.boot.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.archive.JarCommand;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.boot.cli.command.run.RunCommand;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.util.FileCopyUtils;

/**
 * {@link TestRule} that can be used to invoke CLI commands.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class CliTester implements TestRule {

	private final OutputCapture outputCapture = new OutputCapture();

	private long timeout = TimeUnit.MINUTES.toMillis(6);

	private final List<AbstractCommand> commands = new ArrayList<>();

	private final String prefix;

	public CliTester(String prefix) {
		this.prefix = prefix;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public String run(String... args) throws Exception {
		List<String> updatedArgs = new ArrayList<>();
		boolean classpathUpdated = false;
		for (String arg : args) {
			if (arg.startsWith("--classpath=")) {
				arg = arg + ":" + new File("target/test-classes").getAbsolutePath();
				classpathUpdated = true;
			}
			updatedArgs.add(arg);
		}
		if (!classpathUpdated) {
			updatedArgs.add(
					"--classpath=.:" + new File("target/test-classes").getAbsolutePath());
		}
		Future<RunCommand> future = submitCommand(new RunCommand(),
				updatedArgs.toArray(new String[updatedArgs.size()]));
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

	private <T extends OptionParsingCommand> Future<T> submitCommand(final T command,
			String... args) {
		clearUrlHandler();
		final String[] sources = getSources(args);
		return Executors.newSingleThreadExecutor().submit(() -> {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			System.setProperty("server.port", "0");
			System.setProperty("spring.application.class.name",
					"org.springframework.boot.cli.CliTesterSpringApplication");
			System.setProperty("portfile",
					new File("target/server.port").getAbsolutePath());
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
		String output = this.outputCapture.toString();
		this.outputCapture.reset();
		return output;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		final Statement statement = CliTester.this.outputCapture
				.apply(new RunLauncherStatement(base), description);
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				Assume.assumeTrue(
						"Not running sample integration tests because integration profile not active",
						System.getProperty("spring.profiles.active", "integration")
								.contains("integration"));
				statement.evaluate();
			}
		};
	}

	public String getHttpOutput() {
		return getHttpOutput("/");
	}

	public String getHttpOutput(String uri) {
		try {
			int port = Integer.parseInt(
					FileCopyUtils.copyToString(new FileReader("target/server.port")));
			InputStream stream = URI.create("http://localhost:" + port + uri).toURL()
					.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			StringBuilder result = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			return result.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private final class RunLauncherStatement extends Statement {

		private final Statement base;

		private RunLauncherStatement(Statement base) {
			this.base = base;
		}

		@Override
		public void evaluate() throws Throwable {
			System.setProperty("disableSpringSnapshotRepos", "false");
			try {
				try {
					this.base.evaluate();
				}
				finally {
					for (AbstractCommand command : CliTester.this.commands) {
						if (command != null && command instanceof RunCommand) {
							((RunCommand) command).stop();
						}
					}
					System.clearProperty("disableSpringSnapshotRepos");
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
