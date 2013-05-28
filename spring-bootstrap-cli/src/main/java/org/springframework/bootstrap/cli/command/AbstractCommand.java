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

import java.io.IOException;
import java.io.PrintStream;

import org.springframework.bootstrap.cli.Command;

/**
 * Abstract {@link Command} implementation.
 * 
 * @author Phillip Webb
 */
public abstract class AbstractCommand implements Command {

	private String name;

	private boolean optionCommand;

	private String description;

	/**
	 * Create a new {@link AbstractCommand} instance.
	 * @param name the name of the command
	 * @param description the command description
	 */
	public AbstractCommand(String name, String description) {
		this(name, description, false);
	}

	/**
	 * Create a new {@link AbstractCommand} instance.
	 * @param name the name of the command
	 * @param description the command description
	 * @param optionCommand if this command is an option command (see
	 * {@link Command#isOptionCommand()}
	 */
	public AbstractCommand(String name, String description, boolean optionCommand) {
		this.name = name;
		this.description = description;
		this.optionCommand = optionCommand;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public boolean isOptionCommand() {
		return this.optionCommand;
	}

	@Override
	public String getUsageHelp() {
		return null;
	}

	@Override
	public void printHelp(PrintStream out) throws IOException {
	}

}
