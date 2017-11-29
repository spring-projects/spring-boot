/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.cli.command.shell;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.loader.tools.RunProcess;

/**
 * Special {@link Command} used to run a process from the shell. NOTE: this command is not
 * directly installed into the shell.
 *
 * @author Phillip Webb
 */
class RunProcessCommand extends AbstractCommand {

	private final String[] command;

	private volatile RunProcess process;

	RunProcessCommand(String... command) {
		super(null, null);
		this.command = command;
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		return run(Arrays.asList(args));
	}

	protected ExitStatus run(Collection<String> args) throws IOException {
		this.process = new RunProcess(this.command);
		int code = this.process.run(true, args.toArray(new String[args.size()]));
		if (code == 0) {
			return ExitStatus.OK;
		}
		else {
			return new ExitStatus(code, "EXTERNAL_ERROR");
		}
	}

	public boolean handleSigInt() {
		return this.process.handleSigInt();
	}

}
