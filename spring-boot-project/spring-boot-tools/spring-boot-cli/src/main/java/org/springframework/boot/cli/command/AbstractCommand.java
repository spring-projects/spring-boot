/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.cli.command;

import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.cli.command.options.OptionHelp;

/**
 * Abstract {@link Command} implementation.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class AbstractCommand implements Command {

	private final String name;

	private final String description;

	/**
	 * Create a new {@link AbstractCommand} instance.
	 * @param name the name of the command
	 * @param description the command description
	 */
	protected AbstractCommand(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/**
     * Returns the name of the command.
     *
     * @return the name of the command
     */
    @Override
	public String getName() {
		return this.name;
	}

	/**
     * Returns the description of the command.
     *
     * @return the description of the command
     */
    @Override
	public String getDescription() {
		return this.description;
	}

	/**
     * Returns the usage help for this command.
     * 
     * @return the usage help for this command, or null if no usage help is available
     */
    @Override
	public String getUsageHelp() {
		return null;
	}

	/**
     * Returns the help information for this command.
     *
     * @return the help information for this command
     */
    @Override
	public String getHelp() {
		return null;
	}

	/**
     * Returns the help information for the options of this command.
     *
     * @return the collection of OptionHelp objects representing the help information for the options
     */
    @Override
	public Collection<OptionHelp> getOptionsHelp() {
		return Collections.emptyList();
	}

	/**
     * Returns a collection of help examples for this command.
     *
     * @return a collection of help examples, or null if no examples are available
     */
    @Override
	public Collection<HelpExample> getExamples() {
		return null;
	}

}
