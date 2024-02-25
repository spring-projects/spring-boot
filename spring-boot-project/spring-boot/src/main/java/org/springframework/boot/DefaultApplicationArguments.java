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

package org.springframework.boot;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ApplicationArguments}.
 *
 * @author Phillip Webb
 * @since 1.4.1
 */
public class DefaultApplicationArguments implements ApplicationArguments {

	private final Source source;

	private final String[] args;

	/**
	 * Constructs a new DefaultApplicationArguments object with the given arguments.
	 * @param args the command line arguments
	 * @throws IllegalArgumentException if the args parameter is null
	 */
	public DefaultApplicationArguments(String... args) {
		Assert.notNull(args, "Args must not be null");
		this.source = new Source(args);
		this.args = args;
	}

	/**
	 * Returns the source arguments passed to the application.
	 * @return an array of strings representing the source arguments
	 */
	@Override
	public String[] getSourceArgs() {
		return this.args;
	}

	/**
	 * Returns a set of option names.
	 * @return a set of option names
	 */
	@Override
	public Set<String> getOptionNames() {
		String[] names = this.source.getPropertyNames();
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
	}

	/**
	 * Determine if the given option name is present in the application arguments.
	 * @param name the name of the option to check
	 * @return {@code true} if the option is present, {@code false} otherwise
	 */
	@Override
	public boolean containsOption(String name) {
		return this.source.containsProperty(name);
	}

	/**
	 * Returns the option values associated with the specified option name.
	 * @param name the name of the option
	 * @return the option values as an unmodifiable list, or null if the option does not
	 * exist
	 */
	@Override
	public List<String> getOptionValues(String name) {
		List<String> values = this.source.getOptionValues(name);
		return (values != null) ? Collections.unmodifiableList(values) : null;
	}

	/**
	 * Returns the list of non-option arguments.
	 * @return the list of non-option arguments
	 */
	@Override
	public List<String> getNonOptionArgs() {
		return this.source.getNonOptionArgs();
	}

	/**
	 * Source class.
	 */
	private static class Source extends SimpleCommandLinePropertySource {

		/**
		 * Constructs a new Source object with the specified arguments.
		 * @param args the arguments to be passed to the superclass constructor
		 */
		Source(String[] args) {
			super(args);
		}

		/**
		 * Returns the list of non-option arguments.
		 * @return the list of non-option arguments
		 */
		@Override
		public List<String> getNonOptionArgs() {
			return super.getNonOptionArgs();
		}

		/**
		 * Retrieves the option values associated with the specified name.
		 * @param name the name of the option
		 * @return a list of option values
		 */
		@Override
		public List<String> getOptionValues(String name) {
			return super.getOptionValues(name);
		}

	}

}
