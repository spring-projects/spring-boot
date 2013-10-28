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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.command.CleanCommand;
import org.springframework.boot.cli.command.RunCommand;

/**
 * {@link TestRule} that can be used to invoke CLI commands.
 * 
 * @author Phillip Webb
 */
public class CliTester implements TestRule {

	private OutputCapture outputCapture = new OutputCapture();

	private long timeout = TimeUnit.MINUTES.toMillis(6);

	private List<RunCommand> commands = new ArrayList<RunCommand>();

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public String run(final String... args) throws Exception {
		Future<RunCommand> future = Executors.newSingleThreadExecutor().submit(
				new Callable<RunCommand>() {
					@Override
					public RunCommand call() throws Exception {
						RunCommand command = new RunCommand();
						command.run(args);
						return command;
					}
				});
		this.commands.add(future.get(this.timeout, TimeUnit.MILLISECONDS));
		return getOutput();
	}

	public String getOutput() {
		return this.outputCapture.toString();
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		return this.outputCapture.apply(new RunLauncherStatement(base), description);
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
					for (RunCommand command : CliTester.this.commands) {
						if (command != null) {
							command.stop();
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
