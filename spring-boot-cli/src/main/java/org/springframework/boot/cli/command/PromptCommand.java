package org.springframework.boot.cli.command;

import org.springframework.boot.cli.command.AbstractCommand;

/**
 * @author Dave Syer
 */
public class PromptCommand extends AbstractCommand {

	private final ShellCommand runCmd;

	public PromptCommand(ShellCommand runCmd) {
		super("prompt", "Change the prompt used with the current 'shell' command. Execute with no arguments to return to the previous value.");
		this.runCmd = runCmd;
	}

	@Override
	public void run(String... strings) throws Exception {
		if (strings.length > 0) {
			for (String string : strings) {
				runCmd.pushPrompt(string + " ");				
			}
		} else {
			runCmd.popPrompt();
		}
	}

}
