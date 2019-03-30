/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandFactory;
import org.springframework.boot.cli.command.archive.JarCommand;
import org.springframework.boot.cli.command.archive.WarCommand;
import org.springframework.boot.cli.command.core.VersionCommand;
import org.springframework.boot.cli.command.encodepassword.EncodePasswordCommand;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.boot.cli.command.init.InitCommand;
import org.springframework.boot.cli.command.install.InstallCommand;
import org.springframework.boot.cli.command.install.UninstallCommand;
import org.springframework.boot.cli.command.run.RunCommand;

/**
 * Default implementation of {@link CommandFactory}.
 *
 * @author Dave Syer
 */
public class DefaultCommandFactory implements CommandFactory {

	private static final List<Command> DEFAULT_COMMANDS;

	static {
		List<Command> defaultCommands = new ArrayList<>();
		defaultCommands.add(new VersionCommand());
		defaultCommands.add(new RunCommand());
		defaultCommands.add(new GrabCommand());
		defaultCommands.add(new JarCommand());
		defaultCommands.add(new WarCommand());
		defaultCommands.add(new InstallCommand());
		defaultCommands.add(new UninstallCommand());
		defaultCommands.add(new InitCommand());
		defaultCommands.add(new EncodePasswordCommand());
		DEFAULT_COMMANDS = Collections.unmodifiableList(defaultCommands);
	}

	@Override
	public Collection<Command> getCommands() {
		return DEFAULT_COMMANDS;
	}

}
