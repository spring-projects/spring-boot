/*
 * Copyright 2012-present the original author or authors.
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

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.jspecify.annotations.Nullable;

/**
 * Parse and expose arguments specified in a single string.
 *
 * @author Stephane Nicoll
 */
class RunArguments {

	private static final String[] NO_ARGS = {};

	private final Deque<String> args = new LinkedList<>();

	RunArguments(@Nullable String arguments) {
		this(parseArgs(arguments));
	}

	@SuppressWarnings("NullAway") // Maven can't handle nullable arrays
	RunArguments(@Nullable String[] args) {
		this((args != null) ? Arrays.asList(args) : null);
	}

	RunArguments(@Nullable Iterable<@Nullable String> args) {
		if (args != null) {
			for (String arg : args) {
				if (arg != null) {
					this.args.add(arg);
				}
			}
		}
	}

	Deque<String> getArgs() {
		return this.args;
	}

	String[] asArray() {
		return this.args.toArray(new String[0]);
	}

	static String[] parseArgs(@Nullable String arguments) {
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
