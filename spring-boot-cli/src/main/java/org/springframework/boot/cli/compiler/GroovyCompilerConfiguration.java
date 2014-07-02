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

package org.springframework.boot.cli.compiler;

import java.util.List;

import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * Configuration for the {@link GroovyCompiler}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public interface GroovyCompilerConfiguration {

	/**
	 * Constant to be used when there is no {@link #getClasspath() classpath}.
	 */
	public static final String[] DEFAULT_CLASSPATH = { "." };

	/**
	 * Returns the scope in which the compiler operates.
	 */
	GroovyCompilerScope getScope();

	/**
	 * Returns if import declarations should be guessed.
	 */
	boolean isGuessImports();

	/**
	 * Returns if jar dependencies should be guessed.
	 */
	boolean isGuessDependencies();

	/**
	 * Returns true if autoconfiguration transformations should be applied.
	 */
	boolean isAutoconfigure();

	/**
	 * @return a path for local resources
	 */
	String[] getClasspath();

	/**
	 * @return the configuration for the repositories that will be used by the compiler to
	 * resolve dependencies.
	 */
	List<RepositoryConfiguration> getRepositoryConfiguration();

}
