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

import java.io.File;
import java.util.List;

import joptsimple.OptionSet;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerConfigurationAdapter;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.util.StringUtils;

/**
 * {@link Command} to grab the dependencies of one or more Groovy scripts
 * 
 * @author Andy Wilkinson
 */
public class GrabCommand extends OptionParsingCommand {

	public GrabCommand() {
		super("grab", "Download a spring groovy script's dependencies to ./repository",
				new GrabOptionHandler());
	}

	private static final class GrabOptionHandler extends CompilerOptionHandler {

		@Override
		protected void run(OptionSet options) throws Exception {
			FileOptions fileOptions = new FileOptions(options);

			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();

			GroovyCompilerConfiguration configuration = new GroovyCompilerConfigurationAdapter(
					options, this, repositoryConfiguration);

			if (System.getProperty("grape.root") == null) {
				System.setProperty("grape.root", ".");
			}

			if (!new File(System.getProperty("grape.root")).equals(getM2HomeDirectory())) {
				GrabCommand.addDefaultCacheAsRespository(repositoryConfiguration);
			}
			GroovyCompiler groovyCompiler = new GroovyCompiler(configuration);
			groovyCompiler.compile(fileOptions.getFilesArray());
		}

	}

	/**
	 * Add the default local M2 cache directory as a remote repository. Only do this if
	 * the local cache location has been changed from the default.
	 * 
	 * @param repositoryConfiguration
	 */
	public static void addDefaultCacheAsRespository(
			List<RepositoryConfiguration> repositoryConfiguration) {
		repositoryConfiguration.add(0, new RepositoryConfiguration("local", new File(
				getM2HomeDirectory(), "repository").toURI(), true));
	}

	private static File getM2HomeDirectory() {
		String mavenRoot = System.getProperty("maven.home");
		if (StringUtils.hasLength(mavenRoot)) {
			return new File(mavenRoot);
		}
		return new File(System.getProperty("user.home"), ".m2");
	}

}
