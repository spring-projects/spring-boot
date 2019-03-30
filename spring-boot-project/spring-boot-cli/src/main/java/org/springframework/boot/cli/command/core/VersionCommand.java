/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.cli.command.core;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;

/**
 * {@link Command} to display the 'version' number.
 *
 * @author Phillip Webb
 */
public class VersionCommand extends AbstractCommand {

	public VersionCommand() {
		super("version", "Show the version");
	}

	@Override
	public ExitStatus run(String... args) {
		Log.info("Spring CLI v" + getClass().getPackage().getImplementationVersion());
		return ExitStatus.OK;
	}

}
