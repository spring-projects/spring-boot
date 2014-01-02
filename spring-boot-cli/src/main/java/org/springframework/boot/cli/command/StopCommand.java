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

import org.springframework.boot.cli.Command;

/**
 * {@link Command} to stop an application started from the {@link ShellCommand shell}.
 * 
 * @author Jon Brisbin
 */
public class StopCommand extends AbstractCommand {

	private final RunCommand runCmd;

	public StopCommand(RunCommand runCmd) {
		super("stop", "Stop the currently-running application started with "
				+ "the 'run' command.");
		this.runCmd = runCmd;
	}

	@Override
	public void run(String... strings) throws Exception {
		this.runCmd.stop();
	}

}
