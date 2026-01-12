/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.cli.command.shell;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.loader.tools.RunProcess;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Special {@link Command} used to run a process from the shell. NOTE: this command is not
 * directly installed into the shell.
 *
 * @author Phillip Webb
 */
class RunProcessCommand extends AbstractCommand {

	private final String[] command;

	private volatile @Nullable RunProcess process;

	RunProcessCommand(String... command) {
		super("", "");
		this.command = command;
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		return run(Arrays.asList(args));
	}

	protected ExitStatus run(Collection<String> args) throws IOException {
		RunProcess process = new RunProcess(this.command);
		this.process = process;
		int code = process.run(true, StringUtils.toStringArray(args));
		if (code == 0) {
			return ExitStatus.OK;
		}
		else {
			return new ExitStatus(code, "EXTERNAL_ERROR");
		}
	}

	boolean handleSigInt() {
		RunProcess process = this.process;
		Assert.state(process != null, "'process' must not be null");
		return process.handleSigInt();
	}

}
