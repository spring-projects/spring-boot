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

package org.springframework.boot.maven;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Parse and expose arguments specified in a single string.
 *
 * @author Stephane Nicoll
 */
class RunArguments {

	private static final String[] NO_ARGS = {};

	private final Deque<String> args = new LinkedList<>();

	/**
	 * Constructs a new RunArguments object with the given arguments.
	 * @param arguments the string representation of the arguments
	 */
	RunArguments(String arguments) {
		this(parseArgs(arguments));
	}

	/**
	 * This method takes an array of arguments and adds them to the args list.
	 * @param args the array of arguments to be added to the args list
	 */
	RunArguments(String[] args) {
		if (args != null) {
			Arrays.stream(args).filter(Objects::nonNull).forEach(this.args::add);
		}
	}

	/**
	 * Returns the arguments stored in the deque.
	 * @return the arguments stored in the deque
	 */
	Deque<String> getArgs() {
		return this.args;
	}

	/**
	 * Converts the list of arguments to an array of strings.
	 * @return an array of strings containing the arguments
	 */
	String[] asArray() {
		return this.args.toArray(new String[0]);
	}

	/**
	 * Parses the given arguments string and returns an array of parsed arguments.
	 * @param arguments the arguments string to be parsed
	 * @return an array of parsed arguments
	 * @throws IllegalArgumentException if the arguments string is null or empty, or if
	 * parsing fails
	 */
	private static String[] parseArgs(String arguments) {
		if (arguments == null || arguments.trim().isEmpty()) {
			return NO_ARGS;
		}
		try {
			arguments = arguments.replace('\n', ' ').replace('\t', ' ');
			return CommandLineUtils.translateCommandline(arguments);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to parse arguments [" + arguments + "]", ex);
		}
	}

}
