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
 * Base class for any {@link Command}s that use an {@link OptionHandler}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class OptionParsingCommand extends AbstractCommand {

	private OptionHandler handler;

	public OptionParsingCommand(String name, String description, OptionHandler handler) {
		super(name, description);
		this.handler = handler;
	}

	@Override
	public void printHelp(PrintStream out) throws IOException {
		this.handler.printHelp(out);
	}

	@Override
	public final void run(String... args) throws Exception {
		this.handler.run(args);
	}

	protected OptionHandler getHandler() {
		return this.handler;
	}

}
