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
package org.springframework.bootstrap.cli.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

/**
 * @author Dave Syer
 * 
 */
public abstract class OptionHandler {

	private OptionParser parser;

	public OptionSpecBuilder option(String name, String description) {
		return getParser().accepts(name, description);
	}

	public OptionSpecBuilder option(Collection<String> aliases, String description) {
		return getParser().acceptsAll(aliases, description);
	}

	private OptionParser getParser() {
		if (this.parser == null) {
			this.parser = new OptionParser();
			options();
		}
		return this.parser;
	}

	protected abstract void options();

	public final void run(String... args) throws Exception {
		OptionSet options = getParser().parse(args);
		run(options);
	}

	protected abstract void run(OptionSet options) throws Exception;

	public String getHelp() {
		OutputStream out = new ByteArrayOutputStream();
		try {
			getParser().printHelpOn(out);
		} catch (IOException e) {
			return "Help not available";
		}
		return out.toString();
	}

}
