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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandFactory;
import org.springframework.boot.cli.command.core.VersionCommand;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.boot.cli.command.install.InstallCommand;
import org.springframework.boot.cli.command.install.UninstallCommand;
import org.springframework.boot.cli.command.jar.JarCommand;
import org.springframework.boot.cli.command.run.RunCommand;
import org.springframework.boot.cli.command.test.TestCommand;

/**
 * Default implementation of {@link CommandFactory}.
 *
 * @author Dave Syer
 */
public class DefaultCommandFactory implements CommandFactory {

	private static final List<Command> DEFAULT_COMMANDS = Arrays.<Command> asList(
			new VersionCommand(), new RunCommand(), new TestCommand(), new GrabCommand(),
			new JarCommand(), new InstallCommand(), new UninstallCommand());

	@Override
	public Collection<Command> getCommands() {
		return DEFAULT_COMMANDS;
	}

}
