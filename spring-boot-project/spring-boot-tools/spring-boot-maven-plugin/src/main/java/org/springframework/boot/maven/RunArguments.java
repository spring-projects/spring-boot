/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;

import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Parse and expose arguments specified in a single string.
 *
 * @author Stephane Nicoll
 * @since 1.1.0
 */
class RunArguments {

	private static final String[] NO_ARGS = {};

	private final LinkedList<String> args = new LinkedList<>();

	RunArguments(String arguments) {
		this(parseArgs(arguments));
	}

	RunArguments(String[] args) {
		if (args != null) {
			Arrays.stream(args).filter(Objects::nonNull).forEach(this.args::add);
		}
	}

	public LinkedList<String> getArgs() {
		return this.args;
	}

	public String[] asArray() {
		return this.args.toArray(new String[0]);
	}

	private static String[] parseArgs(String arguments) {
		if (arguments == null || arguments.trim().isEmpty()) {
			return NO_ARGS;
		}
		try {
			arguments = arguments.replace('\n', ' ').replace('\t', ' ');
			return CommandLineUtils.translateCommandline(arguments);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(
					"Failed to parse arguments [" + arguments + "]", ex);
		}
	}

}
