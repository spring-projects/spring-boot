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

import java.util.ServiceLoader;

import org.springframework.boot.cli.command.CommandFactory;
import org.springframework.boot.cli.command.CommandRunner;
import org.springframework.boot.cli.command.core.HelpCommand;
import org.springframework.boot.cli.command.core.HintCommand;
import org.springframework.boot.cli.command.core.VersionCommand;
import org.springframework.boot.cli.command.shell.ShellCommand;

/**
 * Spring Command Line Interface. This is the main entry-point for the Spring command line
 * application.
 * 
 * @author Phillip Webb
 * @see #main(String...)
 * @see CommandRunner
 */
public class SpringCli {

	public static void main(String... args) {
		System.setProperty("java.awt.headless", Boolean.toString(true));

		CommandRunner runner = new CommandRunner("spring");
		runner.addCommand(new HelpCommand(runner));
		addServiceLoaderCommands(runner);
		runner.addCommand(new ShellCommand());
		runner.addCommand(new HintCommand(runner));
		runner.setOptionCommands(HelpCommand.class, VersionCommand.class);

		int exitCode = runner.runAndHandleErrors(args);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}

	private static void addServiceLoaderCommands(CommandRunner runner) {
		ServiceLoader<CommandFactory> factories = ServiceLoader.load(
				CommandFactory.class, runner.getClass().getClassLoader());
		for (CommandFactory factory : factories) {
			runner.addCommands(factory.getCommands());
		}
	}

}
