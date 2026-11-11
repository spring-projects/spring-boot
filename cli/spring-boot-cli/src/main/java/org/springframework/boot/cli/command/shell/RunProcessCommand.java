/*
 * Copyright 2012-present the original author or authors.
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
