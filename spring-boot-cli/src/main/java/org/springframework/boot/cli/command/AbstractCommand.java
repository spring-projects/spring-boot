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

package org.springframework.boot.cli.command;

import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.cli.command.options.OptionHelp;

/**
 * Abstract {@link Command} implementation.
 * 
 * @author Phillip Webb
 * @author Dave Syer
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

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String getUsageHelp() {
		return null;
	}

	@Override
	public String getHelp() {
		return null;
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		return Collections.emptyList();
	}

}
