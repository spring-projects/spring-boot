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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.OptionHelp;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Decorate an existing command to run it by forking the current java process.
 * 
 * @author Phillip Webb
 */
class ForkProcessCommand extends RunProcessCommand {

	private static final String MAIN_CLASS = "org.springframework.boot.loader.JarLauncher";

	private final Command command;

	public ForkProcessCommand(Command command) {
		super(getJavaCommand());
		this.command = command;
	}

	@Override
	public String getName() {
		return this.command.getName();
	}

	@Override
	public String getDescription() {
		return this.command.getDescription();
	}

	@Override
	public String getUsageHelp() {
		return this.command.getUsageHelp();
	}

	@Override
	public String getHelp() {
		return this.command.getHelp();
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		return this.command.getOptionsHelp();
	}

	@Override
	public void run(String... args) throws Exception {
		List<String> fullArgs = new ArrayList<String>();
		fullArgs.add("-cp");
		fullArgs.add(System.getProperty("java.class.path"));
		fullArgs.add(MAIN_CLASS);
		fullArgs.add(this.command.getName());
		fullArgs.addAll(Arrays.asList(args));
		run(fullArgs);
	}

	private static String getJavaCommand() {
		String javaHome = System.getProperty("java.home");
		Assert.state(StringUtils.hasLength(javaHome),
				"Unable to find java command to fork process");
		try {
			return getJavaCommand(javaHome).getCanonicalPath();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static File getJavaCommand(String javaHome) {
		File bin = new File(new File(javaHome), "bin");
		File command = new File(bin, "java.exe");
		command = (command.exists() ? command : new File(bin, "java"));
		Assert.state(command.exists(), "Unable to find java in " + javaHome);
		return command;
	}

}
