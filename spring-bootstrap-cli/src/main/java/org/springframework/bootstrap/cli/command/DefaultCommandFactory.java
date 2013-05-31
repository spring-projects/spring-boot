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

package org.springframework.bootstrap.cli.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.bootstrap.cli.Command;
import org.springframework.bootstrap.cli.CommandFactory;

/**
 * Default implementation of {@link CommandFactory}.
 * 
 * @author Dave Syer
 */
public class DefaultCommandFactory implements CommandFactory {

	private static final List<Command> DEFAULT_COMMANDS = Arrays.<Command> asList(
			new VersionCommand(), new RunCommand(), new CleanCommand());

	@Override
	public Collection<Command> getCommands() {
		return DEFAULT_COMMANDS;
	}

}
