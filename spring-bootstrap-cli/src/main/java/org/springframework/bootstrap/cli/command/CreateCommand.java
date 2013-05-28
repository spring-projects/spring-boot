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

import joptsimple.OptionSet;

import org.springframework.bootstrap.cli.Command;

import static java.util.Arrays.asList;

/**
 * {@link Command} to 'create' a new spring groovy script.
 * 
 * @author Phillip Webb
 */
public class CreateCommand extends OptionParsingCommand {

	public CreateCommand() {
		super("create", "Create an new spring groovy script", new CreateOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <file>";
	}

	private static class CreateOptionHandler extends OptionHandler {

		@Override
		protected void options() {
			option(asList("overwite", "f"), "Overwrite any existing file");
			option("type", "Create a specific application type").withOptionalArg()
					.ofType(String.class).describedAs("web, batch, integration");
		}

		@Override
		protected void run(OptionSet options) {
			throw new IllegalStateException("Not implemented"); // FIXME
		}

	}

}
