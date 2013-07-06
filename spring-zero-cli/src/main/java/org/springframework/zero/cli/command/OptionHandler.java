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

package org.springframework.zero.cli.command;

import groovy.lang.Closure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

/**
 * A handler that parses and handles options.
 * 
 * @author Dave Syer
 * @see OptionParsingCommand
 */
public class OptionHandler {

	private OptionParser parser;

	private Closure<Void> closure;

	public OptionSpecBuilder option(String name, String description) {
		return getParser().accepts(name, description);
	}

	public OptionSpecBuilder option(Collection<String> aliases, String description) {
		return getParser().acceptsAll(aliases, description);
	}

	public OptionParser getParser() {
		if (this.parser == null) {
			this.parser = new OptionParser();
			options();
		}
		return this.parser;
	}

	protected void options() {
		if (this.closure != null) {
			this.closure.call();
		}
	}

	public void setOptions(Closure<Void> closure) {
		this.closure = closure;
	}

	public final void run(String... args) throws Exception {
		OptionSet options = getParser().parse(args);
		run(options);
	}

	protected void run(OptionSet options) throws Exception {
	}

	public String getHelp() {
		OutputStream out = new ByteArrayOutputStream();
		try {
			getParser().printHelpOn(out);
		}
		catch (IOException e) {
			return "Help not available";
		}
		return out.toString();
	}

}
