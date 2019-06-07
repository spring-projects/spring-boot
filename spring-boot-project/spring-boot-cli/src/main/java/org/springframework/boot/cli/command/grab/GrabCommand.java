/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cli.command.grab;

import java.util.List;

import joptsimple.OptionSet;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.CompilerOptionHandler;
import org.springframework.boot.cli.command.options.OptionSetGroovyCompilerConfiguration;
import org.springframework.boot.cli.command.options.SourceOptions;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * {@link Command} to grab the dependencies of one or more Groovy scripts.
 *
 * @author Andy Wilkinson
 */
public class GrabCommand extends OptionParsingCommand {

	public GrabCommand() {
		super("grab", "Download a spring groovy script's dependencies to ./repository", new GrabOptionHandler());
	}

	private static final class GrabOptionHandler extends CompilerOptionHandler {

		@Override
		protected ExitStatus run(OptionSet options) throws Exception {
			SourceOptions sourceOptions = new SourceOptions(options);
			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();
			GroovyCompilerConfiguration configuration = new OptionSetGroovyCompilerConfiguration(options, this,
					repositoryConfiguration);
			if (System.getProperty("grape.root") == null) {
				System.setProperty("grape.root", ".");
			}
			GroovyCompiler groovyCompiler = new GroovyCompiler(configuration);
			groovyCompiler.compile(sourceOptions.getSourcesArray());
			return ExitStatus.OK;
		}

	}

}
