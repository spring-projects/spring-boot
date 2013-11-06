/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.CleanCommand;
import org.springframework.boot.cli.command.RunCommand;
import org.springframework.boot.cli.command.TestCommand;

/**
 * {@link TestRule} that can be used to invoke CLI commands.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class CliTester implements TestRule {

	private OutputCapture outputCapture = new OutputCapture();

	private long timeout = TimeUnit.MINUTES.toMillis(6);

	private List<AbstractCommand> commands = new ArrayList<AbstractCommand>();

	private String prefix;

	public CliTester(String prefix) {
		this.prefix = prefix;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public String run(String... args) throws Exception {
		final String[] sources = getSources(args);
		Future<RunCommand> future = Executors.newSingleThreadExecutor().submit(
				new Callable<RunCommand>() {
					@Override
					public RunCommand call() throws Exception {
						RunCommand command = new RunCommand();
						command.run(sources);
						return command;
					}
				});
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	public String test(String... args) throws Exception {
		final String[] sources = getSources(args);
		Future<TestCommand> future = Executors.newSingleThreadExecutor().submit(
				new Callable<TestCommand>() {
					@Override
					public TestCommand call() throws Exception {
						TestCommand command = new TestCommand();
						command.run(sources);
						return command;
					}
				});
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	protected String[] getSources(String... args) {
		final String[] sources = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			sources[i] = this.prefix + arg;
		}
		return sources;
	}

	public String getOutput() {
		return this.outputCapture.toString();
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				Assume.assumeTrue(
						"Not running sample integration tests because integration profile not active",
						System.getProperty("spring.profiles.active", "integration")
								.contains("integration"));
				CliTester.this.outputCapture.apply(new RunLauncherStatement(base),
						description);
			}
		};
	}

	public String getHttpOutput() {
		return getHttpOutput("http://localhost:8080");
	}

	public String getHttpOutput(String uri) {
		try {
			InputStream stream = URI.create(uri).toURL().openStream();
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
				new CleanCommand().run("org.springframework");
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
