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

import joptsimple.OptionSpec;

import static java.util.Arrays.asList;

/**
 * An {@link OptionHandler} for commands that result in the compilation of one or more
 * Groovy scripts
 * 
 * @author Andy Wilkinson
 */
public class CompilerOptionHandler extends OptionHandler {

	private OptionSpec<Void> noGuessImportsOption;

	private OptionSpec<Void> noGuessDependenciesOption;

	private OptionSpec<String> classpathOption;

	@Override
	protected final void options() {
		this.noGuessImportsOption = option("no-guess-imports",
				"Do not attempt to guess imports");
		this.noGuessDependenciesOption = option("no-guess-dependencies",
				"Do not attempt to guess dependencies");
		this.classpathOption = option(asList("classpath", "cp"),
				"Additional classpath entries").withRequiredArg();
		doOptions();
	}

	protected void doOptions() {
	}

	public OptionSpec<Void> getNoGuessImportsOption() {
		return this.noGuessImportsOption;
	}

	public OptionSpec<Void> getNoGuessDependenciesOption() {
		return this.noGuessDependenciesOption;
	}

	public OptionSpec<String> getClasspathOption() {
		return this.classpathOption;
	}

}
