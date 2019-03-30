/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.springframework.boot.cli.command.CommandFactory;
import org.springframework.boot.cli.command.CommandRunner;
import org.springframework.boot.cli.command.core.HelpCommand;
import org.springframework.boot.cli.command.core.HintCommand;
import org.springframework.boot.cli.command.core.VersionCommand;
import org.springframework.boot.cli.command.shell.ShellCommand;
import org.springframework.boot.loader.tools.LogbackInitializer;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Spring Command Line Interface. This is the main entry-point for the Spring command line
 * application.
 *
 * @author Phillip Webb
 * @see #main(String...)
 * @see CommandRunner
 */
public final class SpringCli {

	private SpringCli() {
	}

	public static void main(String... args) {
		System.setProperty("java.awt.headless", Boolean.toString(true));
		LogbackInitializer.initialize();

		CommandRunner runner = new CommandRunner("spring");
		ClassUtils.overrideThreadContextClassLoader(createExtendedClassLoader(runner));
		runner.addCommand(new HelpCommand(runner));
		addServiceLoaderCommands(runner);
		runner.addCommand(new ShellCommand());
		runner.addCommand(new HintCommand(runner));
		runner.setOptionCommands(HelpCommand.class, VersionCommand.class);
		runner.setHiddenCommands(HintCommand.class);

		int exitCode = runner.runAndHandleErrors(args);
		if (exitCode != 0) {
			// If successful, leave it to run in case it's a server app
			System.exit(exitCode);
		}
	}

	private static void addServiceLoaderCommands(CommandRunner runner) {
		ServiceLoader<CommandFactory> factories = ServiceLoader
				.load(CommandFactory.class);
		for (CommandFactory factory : factories) {
			runner.addCommands(factory.getCommands());
		}
	}

	private static URLClassLoader createExtendedClassLoader(CommandRunner runner) {
		return new URLClassLoader(getExtensionURLs(), runner.getClass().getClassLoader());
	}

	private static URL[] getExtensionURLs() {
		List<URL> urls = new ArrayList<>();
		String home = SystemPropertyUtils
				.resolvePlaceholders("${spring.home:${SPRING_HOME:.}}");
		File extDirectory = new File(new File(home, "lib"), "ext");
		if (extDirectory.isDirectory()) {
			for (File file : extDirectory.listFiles()) {
				if (file.getName().endsWith(".jar")) {
					try {
						urls.add(file.toURI().toURL());
					}
					catch (MalformedURLException ex) {
						throw new IllegalStateException(ex);
					}
				}
			}
		}
		return urls.toArray(new URL[0]);
	}

}
