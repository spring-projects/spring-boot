/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.regex.Pattern;

/**
 * Regular Expressions for image names and references based on those found in the Docker
 * codebase.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see <a href=
 * "https://github.com/docker/distribution/blob/master/reference/reference.go">Docker
 * grammar reference</a>
 * @see <a href=
 * "https://github.com/docker/distribution/blob/master/reference/regexp.go">Docker grammar
 * implementation</a>
 * @see <a href=
 * "https://stackoverflow.com/questions/37861791/how-are-docker-image-names-parsed">How
 * are Docker image names parsed?</a>
 */
final class Regex implements CharSequence {

	static final Pattern DOMAIN;
	static {
		Regex component = Regex.oneOf("[a-zA-Z0-9]", "[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]");
		Regex dotComponent = Regex.group("[.]", component);
		Regex colonPort = Regex.of("[:][0-9]+");
		Regex dottedDomain = Regex.group(component, dotComponent.oneOrMoreTimes());
		Regex dottedDomainAndPort = Regex.group(component, dotComponent.oneOrMoreTimes(), colonPort);
		Regex nameAndPort = Regex.group(component, colonPort);
		DOMAIN = Regex.oneOf(dottedDomain, nameAndPort, dottedDomainAndPort, "localhost").compile();
	}

	private static final Regex PATH_COMPONENT;
	static {
		Regex segment = Regex.of("[a-z0-9]+");
		Regex separator = Regex.group("[._]|__|[-]*");
		Regex separatedSegment = Regex.group(separator, segment).oneOrMoreTimes();
		PATH_COMPONENT = Regex.of(segment, Regex.group(separatedSegment).zeroOrOnce());
	}

	static final Pattern PATH;
	static {
		Regex component = PATH_COMPONENT;
		Regex slashComponent = Regex.group("[/]", component);
		Regex slashComponents = Regex.group(slashComponent.oneOrMoreTimes());
		PATH = Regex.of(component, slashComponents.zeroOrOnce()).compile();
	}

	static final Pattern TAG = Regex.of("^[\\w][\\w.-]{0,127}").compile();

	static final Pattern DIGEST = Regex.of("^[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][[A-Fa-f0-9]]{32,}")
		.compile();

	private final String value;

	/**
	 * Constructs a new Regex object with the specified value.
	 * @param value the character sequence to be used as the value for the Regex object
	 */
	private Regex(CharSequence value) {
		this.value = value.toString();
	}

	/**
	 * Returns a new Regex object that matches one or more occurrences of the current
	 * regex pattern.
	 * @return a new Regex object that matches one or more occurrences of the current
	 * regex pattern
	 */
	private Regex oneOrMoreTimes() {
		return new Regex(this.value + "+");
	}

	/**
	 * Returns a new Regex object that matches zero or one occurrence of the current regex
	 * pattern.
	 * @return a new Regex object that matches zero or one occurrence of the current regex
	 * pattern
	 */
	private Regex zeroOrOnce() {
		return new Regex(this.value + "?");
	}

	/**
	 * Compiles the regular expression pattern with the given value.
	 * @return the compiled Pattern object
	 */
	Pattern compile() {
		return Pattern.compile("^" + this.value + "$");
	}

	/**
	 * Returns the length of the value in the Regex class.
	 * @return the length of the value
	 */
	@Override
	public int length() {
		return this.value.length();
	}

	/**
	 * Returns the character at the specified index in the string value.
	 * @param index the index of the character to be returned
	 * @return the character at the specified index
	 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index
	 * >= length())
	 */
	@Override
	public char charAt(int index) {
		return this.value.charAt(index);
	}

	/**
	 * Returns a new CharSequence that is a subsequence of this sequence.
	 * @param start the starting index, inclusive
	 * @param end the ending index, exclusive
	 * @return the specified subsequence
	 * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative, if
	 * {@code end} is greater than {@code start}, or if {@code end} is greater than the
	 * length of this sequence
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		return this.value.subSequence(start, end);
	}

	/**
	 * Returns a string representation of the object.
	 * @return the string representation of the object
	 */
	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Creates a regular expression pattern from the given character sequences.
	 * @param expressions the character sequences to be combined into a regular expression
	 * pattern
	 * @return a regular expression pattern created from the given character sequences
	 */
	private static Regex of(CharSequence... expressions) {
		return new Regex(String.join("", expressions));
	}

	/**
	 * Creates a regular expression pattern that matches any of the given expressions.
	 * @param expressions the expressions to match
	 * @return a regular expression pattern that matches any of the given expressions
	 */
	private static Regex oneOf(CharSequence... expressions) {
		return new Regex("(?:" + String.join("|", expressions) + ")");
	}

	/**
	 * Creates a regular expression group using the provided expressions.
	 * @param expressions the expressions to be grouped
	 * @return a regular expression group
	 */
	private static Regex group(CharSequence... expressions) {
		return new Regex("(?:" + String.join("", expressions) + ")");
	}

}
