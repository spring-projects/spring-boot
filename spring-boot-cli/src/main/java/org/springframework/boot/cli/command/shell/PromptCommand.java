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

package org.springframework.boot.cli.command.shell;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;

/**
 * {@link Command} to change the {@link Shell} prompt.
 * 
 * @author Dave Syer
 */
public class PromptCommand extends AbstractCommand {

	private final Shell shell;

	public PromptCommand(Shell shell) {
		super("prompt", "Change the prompt used with the current 'shell' command. "
				+ "Execute with no arguments to return to the previous value.");
		this.shell = shell;
	}

	@Override
	public void run(String... strings) throws Exception {
		if (strings.length > 0) {
			for (String string : strings) {
				this.shell.pushPrompt(string + " ");
			}
		}
		else {
			this.shell.popPrompt();
		}
	}

}
