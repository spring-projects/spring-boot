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

package org.springframework.boot.cli.command.options;

import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * Simple adapter class to present an {@link OptionSet} as a
 * {@link GroovyCompilerConfiguration}.
 *
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class OptionSetGroovyCompilerConfiguration implements GroovyCompilerConfiguration {

	private final OptionSet options;

	private final CompilerOptionHandler optionHandler;

	private final List<RepositoryConfiguration> repositoryConfiguration;

	protected OptionSetGroovyCompilerConfiguration(OptionSet optionSet, CompilerOptionHandler compilerOptionHandler) {
		this(optionSet, compilerOptionHandler, RepositoryConfigurationFactory.createDefaultRepositoryConfiguration());
	}

	public OptionSetGroovyCompilerConfiguration(OptionSet optionSet, CompilerOptionHandler compilerOptionHandler,
			List<RepositoryConfiguration> repositoryConfiguration) {
		this.options = optionSet;
		this.optionHandler = compilerOptionHandler;
		this.repositoryConfiguration = repositoryConfiguration;
	}

	protected OptionSet getOptions() {
		return this.options;
	}

	@Override
	public GroovyCompilerScope getScope() {
		return GroovyCompilerScope.DEFAULT;
	}

	@Override
	public boolean isGuessImports() {
		return !this.options.has(this.optionHandler.getNoGuessImportsOption());
	}

	@Override
	public boolean isGuessDependencies() {
		return !this.options.has(this.optionHandler.getNoGuessDependenciesOption());
	}

	@Override
	public boolean isAutoconfigure() {
		return this.optionHandler.getAutoconfigureOption().value(this.options);
	}

	@Override
	public String[] getClasspath() {
		OptionSpec<String> classpathOption = this.optionHandler.getClasspathOption();
		if (this.options.has(classpathOption)) {
			return this.options.valueOf(classpathOption).split(":");
		}
		return DEFAULT_CLASSPATH;
	}

	@Override
	public List<RepositoryConfiguration> getRepositoryConfiguration() {
		return this.repositoryConfiguration;
	}

	@Override
	public boolean isQuiet() {
		return false;
	}

}
