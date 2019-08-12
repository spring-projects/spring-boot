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

import java.util.List;
import java.util.Set;

/**
 * Provides access to the arguments that were used to run a {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public interface ApplicationArguments {

	/**
	 * Return the raw unprocessed arguments that were passed to the application.
	 * @return the arguments
	 */
	String[] getSourceArgs();

	/**
	 * Return the names of all option arguments. For example, if the arguments were
	 * "--foo=bar --debug" would return the values {@code ["foo", "debug"]}.
	 * @return the option names or an empty set
	 */
	Set<String> getOptionNames();

	/**
	 * Return whether the set of option arguments parsed from the arguments contains an
	 * option with the given name.
	 * @param name the name to check
	 * @return {@code true} if the arguments contain an option with the given name
	 */
	boolean containsOption(String name);

	/**
	 * Return the collection of values associated with the arguments option having the
	 * given name.
	 * <ul>
	 * <li>if the option is present and has no argument (e.g.: "--foo"), return an empty
	 * collection ({@code []})</li>
	 * <li>if the option is present and has a single value (e.g. "--foo=bar"), return a
	 * collection having one element ({@code ["bar"]})</li>
	 * <li>if the option is present and has multiple values (e.g. "--foo=bar --foo=baz"),
	 * return a collection having elements for each value ({@code ["bar", "baz"]})</li>
	 * <li>if the option is not present, return {@code null}</li>
	 * </ul>
	 * @param name the name of the option
	 * @return a list of option values for the given name
	 */
	List<String> getOptionValues(String name);

	/**
	 * Return the collection of non-option arguments parsed.
	 * @return the non-option arguments or an empty list
	 */
	List<String> getNonOptionArgs();

}
