package org.springframework.boot.cli.command;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.RunCommand;

/**
 * @author Jon Brisbin
 */
public class StopCommand extends AbstractCommand {

	private final RunCommand runCmd;

	public StopCommand(RunCommand runCmd) {
		super("stop", "Stop the currently-running application started with the 'run' command.");
		this.runCmd = runCmd;
	}

	@Override
	public void run(String... strings) throws Exception {
		runCmd.stop();
	}

}
