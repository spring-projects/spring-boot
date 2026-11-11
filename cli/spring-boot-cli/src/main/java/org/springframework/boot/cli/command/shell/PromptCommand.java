/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.shell;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * {@link Command} to change the {@link Shell} prompt.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class PromptCommand extends AbstractCommand {

	private final ShellPrompts prompts;

	public PromptCommand(ShellPrompts shellPrompts) {
		super("prompt", "Change the prompt used with the current 'shell' command. "
				+ "Execute with no arguments to return to the previous value.");
		this.prompts = shellPrompts;
	}

	@Override
	public ExitStatus run(String... strings) throws Exception {
		if (strings.length > 0) {
			for (String string : strings) {
				this.prompts.pushPrompt(string + " ");
			}
		}
		else {
			this.prompts.popPrompt();
		}
		return ExitStatus.OK;
	}

}
