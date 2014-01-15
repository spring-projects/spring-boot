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

import groovy.lang.Closure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

import org.springframework.boot.cli.OptionHelp;

/**
 * Delegate used by {@link OptionParsingCommand} to parse options and run the command.
 * 
 * @author Dave Syer
 * @see OptionParsingCommand
 * @see #run(OptionSet)
 */
public class OptionHandler {

	private OptionParser parser;

	private Closure<?> closure;

	private String help;

	private Collection<OptionHelp> optionHelp;

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
	}

	public void setClosure(Closure<?> closure) {
		this.closure = closure;
	}

	public final void run(String... args) throws Exception {
		String[] argsToUse = args.clone();
		for (int i = 0; i < argsToUse.length; i++) {
			if ("-cp".equals(argsToUse[i])) {
				argsToUse[i] = "--cp";
			}
		}
		OptionSet options = getParser().parse(args);
		run(options);
	}

	/**
	 * Run the command using the specified parsed {@link OptionSet}.
	 * @param options the parsed option set
	 * @throws Exception
	 */
	protected void run(OptionSet options) throws Exception {
		if (this.closure != null) {
			this.closure.call(options);
		}
	}

	public String getHelp() {
		if (this.help == null) {
			getParser().formatHelpWith(new BuiltinHelpFormatter(80, 2));
			OutputStream out = new ByteArrayOutputStream();
			try {
				getParser().printHelpOn(out);
			}
			catch (IOException ex) {
				return "Help not available";
			}
			this.help = out.toString().replace(" --cp ", " -cp  ");
		}
		return this.help;
	}

	public Collection<OptionHelp> getOptionsHelp() {
		if (this.optionHelp == null) {
			OptionHelpFormatter formatter = new OptionHelpFormatter();
			getParser().formatHelpWith(formatter);
			try {
				getParser().printHelpOn(new ByteArrayOutputStream());
			}
			catch (Exception ex) {
				// Ignore and provide no hints
			}
			this.optionHelp = formatter.getOptionHelp();
		}
		return this.optionHelp;
	}

	private static class OptionHelpFormatter implements HelpFormatter {

		private final List<OptionHelp> help = new ArrayList<OptionHelp>();

		@Override
		public String format(Map<String, ? extends OptionDescriptor> options) {
			Comparator<OptionDescriptor> comparator = new Comparator<OptionDescriptor>() {
				@Override
				public int compare(OptionDescriptor first, OptionDescriptor second) {
					return first.options().iterator().next()
							.compareTo(second.options().iterator().next());
				}
			};

			Set<OptionDescriptor> sorted = new TreeSet<OptionDescriptor>(comparator);
			sorted.addAll(options.values());

			for (OptionDescriptor descriptor : sorted) {
				if (!descriptor.representsNonOptions()) {
					this.help.add(new OptionHelpAdapter(descriptor));
				}
			}
			return "";
		}

		public Collection<OptionHelp> getOptionHelp() {
			return Collections.unmodifiableList(this.help);
		}

	}

	private static class OptionHelpAdapter implements OptionHelp {

		private final LinkedHashSet<String> options;

		private final String description;

		public OptionHelpAdapter(OptionDescriptor descriptor) {
			this.options = new LinkedHashSet<String>();
			for (String option : descriptor.options()) {
				this.options.add((option.length() == 1 ? "-" : "--") + option);
			}
			if (this.options.contains("--cp")) {
				this.options.remove("--cp");
				this.options.add("-cp");
			}
			this.description = descriptor.description();
		}

		@Override
		public Set<String> getOptions() {
			return this.options;
		}

		@Override
		public String getUsageHelp() {
			return this.description;
		}
	}
}
