/*
 * Copyright 2012-2014 the original author or authors.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.boot.cli.command.jar.JarCommand;
import org.springframework.boot.cli.command.run.RunCommand;
import org.springframework.boot.cli.command.test.TestCommand;
import org.springframework.boot.cli.util.OutputCapture;
import org.springframework.util.SocketUtils;

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

	private final List<AbstractCommand> commands = new ArrayList<AbstractCommand>();

	private final String prefix;

	private final int port = SocketUtils.findAvailableTcpPort();

	public CliTester(String prefix) {
		this.prefix = prefix;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public String run(String... args) throws Exception {
		Future<RunCommand> future = submitCommand(new RunCommand(), args);
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	public String test(String... args) throws Exception {
		Future<TestCommand> future = submitCommand(new TestCommand(), args);
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
		final String[] sources = getSources(args);
		return Executors.newSingleThreadExecutor().submit(new Callable<T>() {
			@Override
			public T call() throws Exception {
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				System.setProperty("server.port", String.valueOf(CliTester.this.port));
				try {
					command.run(sources);
					return command;
				}
				finally {
					System.clearProperty("server.port");
					Thread.currentThread().setContextClassLoader(loader);
				}
			}
		});
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
				sources[i] = this.prefix + arg;
			}
		}
		return sources;
	}

	public String getOutput() {
		return this.outputCapture.toString();
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		final Statement statement = CliTester.this.outputCapture.apply(
				new RunLauncherStatement(base), description);
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
			InputStream stream = URI.create("http://localhost:" + this.port + uri)
					.toURL().openStream();
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
