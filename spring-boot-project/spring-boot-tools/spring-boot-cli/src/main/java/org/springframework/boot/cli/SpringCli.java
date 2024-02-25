/*
 * Copyright 2012-2022 the original author or authors.
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
 * @since 1.0.0
 * @see #main(String...)
 * @see CommandRunner
 */
public final class SpringCli {

	/**
     * Private constructor for the SpringCli class.
     */
    private SpringCli() {
	}

	/**
     * The main method of the SpringCli class.
     * 
     * This method is the entry point of the Spring CLI application. It initializes the necessary configurations, sets the system property for headless mode, and initializes the Logback logging framework. It creates a CommandRunner instance and adds various commands to it, such as HelpCommand, ShellCommand, and HintCommand. It also sets the option commands and hidden commands for the runner. Finally, it runs the command runner and handles any errors that occur. If the exit code is non-zero, the application exits with that code.
     * 
     * @param args The command line arguments passed to the application.
     */
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

	/**
     * Adds commands from all the implementations of CommandFactory found using ServiceLoader.
     * 
     * @param runner the CommandRunner to add the commands to
     */
    private static void addServiceLoaderCommands(CommandRunner runner) {
		ServiceLoader<CommandFactory> factories = ServiceLoader.load(CommandFactory.class);
		for (CommandFactory factory : factories) {
			runner.addCommands(factory.getCommands());
		}
	}

	/**
     * Creates an extended class loader using the provided CommandRunner.
     * 
     * @param runner the CommandRunner to be used for the class loader
     * @return the created URLClassLoader
     */
    private static URLClassLoader createExtendedClassLoader(CommandRunner runner) {
		return new URLClassLoader(getExtensionURLs(), runner.getClass().getClassLoader());
	}

	/**
     * Retrieves the URLs of the extension JAR files located in the "ext" directory of the Spring home directory.
     * 
     * @return an array of URLs representing the extension JAR files
     * @throws IllegalStateException if a file URL cannot be created for a JAR file
     */
    private static URL[] getExtensionURLs() {
		List<URL> urls = new ArrayList<>();
		String home = SystemPropertyUtils.resolvePlaceholders("${spring.home:${SPRING_HOME:.}}");
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
