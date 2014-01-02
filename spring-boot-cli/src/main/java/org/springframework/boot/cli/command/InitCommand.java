/*
 * Copyright 2012-2013 the original author or authors.
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

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.io.File;
import java.util.List;
import java.util.ServiceLoader;

import joptsimple.OptionSet;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.CommandFactory;
import org.springframework.boot.cli.SpringCli;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerConfigurationAdapter;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * <p>
 * Command to initialize the Spring CLI with commands from the classpath. If the current
 * context class loader is a GroovyClassLoader then it can be enhanced by passing in
 * compiler options (e.g. <code>--classpath=...</code>).
 * </p>
 * <p>
 * If the current context class loader is not already GroovyClassLoader then one will be
 * created and will replace the current context loader. In this case command arguments can
 * include files to compile that have <code>@Grab</code> annotations to process. By
 * default a script called "init.groovy" or "spring.groovy" is used if it exists in the
 * current directory or the root of the classpath.
 * </p>
 * 
 * @author Dave Syer
 */
public class InitCommand extends OptionParsingCommand {

	public static final String NAME = "init";

	public InitCommand(SpringCli cli) {
		super(NAME, "(Re)-initialize the Spring cli", new InitOptionHandler(cli));
	}

	private static class InitOptionHandler extends CompilerOptionHandler {

		private SpringCli cli;
		private GroovyCompiler compiler;

		public InitOptionHandler(SpringCli cli) {
			this.cli = cli;
		}

		@Override
		protected void run(OptionSet options) throws Exception {

			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			boolean enhanced = false;

			FileOptions fileOptions = new FileOptions(options, loader, "init.groovy",
					"spring.groovy");
			File[] files = fileOptions.getFilesArray();

			if (!(loader instanceof GroovyClassLoader)) {

				List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
						.createDefaultRepositoryConfiguration();

				GroovyCompilerConfiguration configuration = new InitGroovyCompilerConfigurationAdapter(
						options, this, repositoryConfiguration);

				this.compiler = new GroovyCompiler(configuration);
				loader = this.compiler.getLoader();
				Thread.currentThread().setContextClassLoader(loader);

			}
			else {
				String classpath = getClasspathOption().value(options);
				if (classpath != null && classpath.length() > 0) {
					((GroovyClassLoader) loader).addClasspath(classpath);
					enhanced = true;
				}
			}

			if (this.compiler != null && files.length > 0) {
				Class<?>[] classes = this.compiler.compile(files);
				for (Class<?> type : classes) {
					if (Script.class.isAssignableFrom(type)) {
						((Script) type.newInstance()).run();
					}
				}
				enhanced = true;
			}

			if (this.cli.getCommands().isEmpty() || enhanced) {

				for (CommandFactory factory : ServiceLoader.load(CommandFactory.class,
						loader)) {
					for (Command command : factory.getCommands(this.cli)) {
						this.cli.register(command);
					}
				}

			}

		}

	}

	private static class InitGroovyCompilerConfigurationAdapter extends
			GroovyCompilerConfigurationAdapter {
		private InitGroovyCompilerConfigurationAdapter(OptionSet optionSet,
				CompilerOptionHandler compilerOptionHandler,
				List<RepositoryConfiguration> repositoryConfiguration) {
			super(optionSet, compilerOptionHandler, repositoryConfiguration);
		}

		@Override
		public GroovyCompilerScope getScope() {
			return GroovyCompilerScope.EXTENSION;
		}
	}

}
