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

package org.springframework.boot.cli;

/**
 * A single command that can be run from the CLI.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see #run(String...)
 */
public interface Command {

	/**
	 * Returns the name of the command.
	 */
	String getName();

	/**
	 * Returns a description of the command.
	 */
	String getDescription();

	/**
	 * Returns usage help for the command. This should be a simple one-line string
	 * describing basic usage. e.g. '[options] &lt;file&gt;'. Do not include the name of
	 * the command in this string.
	 */
	String getUsageHelp();

	/**
	 * Gets full help text for the command, e.g. a longer description and one line per
	 * option.
	 */
	String getHelp();

	/**
	 * Run the command.
	 * @param args command arguments (this will not include the command itself)
	 * @throws Exception
	 */
	void run(String... args) throws Exception;

}
