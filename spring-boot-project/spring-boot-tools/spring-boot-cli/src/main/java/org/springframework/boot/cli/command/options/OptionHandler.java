/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.function.Function;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * Delegate used by {@link OptionParsingCommand} to parse options and run the command.
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see OptionParsingCommand
 * @see #run(OptionSet)
 */
public class OptionHandler {

	private final Function<String, String> argumentProcessor;

	private OptionParser parser;

	private String help;

	private Collection<OptionHelp> optionHelp;

	/**
	 * Create a new {@link OptionHandler} instance.
	 */
	public OptionHandler() {
		this(Function.identity());
	}

	/**
	 * Create a new {@link OptionHandler} instance with an argument processor.
	 * @param argumentProcessor strategy that can be used to manipulate arguments before
	 * they are used.
	 */
	public OptionHandler(Function<String, String> argumentProcessor) {
		this.argumentProcessor = argumentProcessor;
	}

	/**
	 * Creates an OptionSpecBuilder for the specified option name and description.
	 * @param name the name of the option
	 * @param description the description of the option
	 * @return an OptionSpecBuilder for the specified option
	 */
	public OptionSpecBuilder option(String name, String description) {
		return getParser().accepts(name, description);
	}

	/**
	 * Creates an OptionSpecBuilder for the given list of aliases and description.
	 * @param aliases the list of aliases for the option
	 * @param description the description of the option
	 * @return the OptionSpecBuilder for the given aliases and description
	 */
	public OptionSpecBuilder option(List<String> aliases, String description) {
		return getParser().acceptsAll(aliases, description);
	}

	/**
	 * Returns the OptionParser object associated with this OptionHandler. If the
	 * OptionParser object is not yet initialized, it initializes it by calling the
	 * options() method.
	 * @return the OptionParser object associated with this OptionHandler
	 */
	public OptionParser getParser() {
		if (this.parser == null) {
			this.parser = new OptionParser();
			options();
		}
		return this.parser;
	}

	/**
	 * This method is used to handle options.
	 */
	protected void options() {
	}

	/**
	 * Runs the program with the given arguments.
	 * @param args the arguments to be passed to the program
	 * @return the exit status of the program
	 * @throws Exception if an error occurs during program execution
	 */
	public final ExitStatus run(String... args) throws Exception {
		String[] argsToUse = args.clone();
		for (int i = 0; i < argsToUse.length; i++) {
			if ("-cp".equals(argsToUse[i])) {
				argsToUse[i] = "--cp";
			}
			argsToUse[i] = this.argumentProcessor.apply(argsToUse[i]);
		}
		OptionSet options = getParser().parse(argsToUse);
		return run(options);
	}

	/**
	 * Run the command using the specified parsed {@link OptionSet}.
	 * @param options the parsed option set
	 * @return an ExitStatus
	 * @throws Exception in case of errors
	 */
	protected ExitStatus run(OptionSet options) throws Exception {
		return ExitStatus.OK;
	}

	/**
	 * Retrieves the help information for this OptionHandler. If the help information is
	 * not already generated, it generates it by formatting the help using a
	 * BuiltinHelpFormatter with a specified width and indent. It then prints the help to
	 * an output stream and stores the generated help in a string.
	 * @return the help information for this OptionHandler
	 */
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

	/**
	 * Retrieves the help information for the options.
	 * @return the collection of OptionHelp objects containing the help information for
	 * the options
	 */
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

	/**
	 * OptionHelpFormatter class.
	 */
	private static final class OptionHelpFormatter implements HelpFormatter {

		private final List<OptionHelp> help = new ArrayList<>();

		/**
		 * Formats the given options map into a string representation.
		 * @param options the map of options to be formatted
		 * @return the formatted string representation of the options
		 */
		@Override
		public String format(Map<String, ? extends OptionDescriptor> options) {
			Comparator<OptionDescriptor> comparator = Comparator
				.comparing((optionDescriptor) -> optionDescriptor.options().iterator().next());
			Set<OptionDescriptor> sorted = new TreeSet<>(comparator);
			sorted.addAll(options.values());
			for (OptionDescriptor descriptor : sorted) {
				if (!descriptor.representsNonOptions()) {
					this.help.add(new OptionHelpAdapter(descriptor));
				}
			}
			return "";
		}

		/**
		 * Returns an unmodifiable list of OptionHelp objects.
		 * @return the list of OptionHelp objects
		 */
		Collection<OptionHelp> getOptionHelp() {
			return Collections.unmodifiableList(this.help);
		}

	}

	/**
	 * OptionHelpAdapter class.
	 */
	private static class OptionHelpAdapter implements OptionHelp {

		private final Set<String> options;

		private final String description;

		/**
		 * Constructs a new OptionHelpAdapter with the given OptionDescriptor.
		 * @param descriptor the OptionDescriptor to be used
		 */
		OptionHelpAdapter(OptionDescriptor descriptor) {
			this.options = new LinkedHashSet<>();
			for (String option : descriptor.options()) {
				String prefix = (option.length() != 1) ? "--" : "-";
				this.options.add(prefix + option);
			}
			if (this.options.contains("--cp")) {
				this.options.remove("--cp");
				this.options.add("-cp");
			}
			this.description = descriptor.description();
		}

		/**
		 * Returns the set of options available.
		 * @return the set of options available
		 */
		@Override
		public Set<String> getOptions() {
			return this.options;
		}

		/**
		 * Returns the usage help for this method.
		 * @return the usage help as a string
		 */
		@Override
		public String getUsageHelp() {
			return this.description;
		}

	}

}
