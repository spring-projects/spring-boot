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

package org.springframework.boot.info;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.info.GitProperties.GitPropertiesRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Provide git-related information such as commit id and time.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ImportRuntimeHints(GitPropertiesRuntimeHints.class)
public class GitProperties extends InfoProperties {

	static final Set<Coercer> coercers = Set.of(Coercer.milliseconds(),
			Coercer.dateTimePattern("yyyy-MM-dd'T'HH:mm:ssXXX"), Coercer.dateTimePattern("yyyy-MM-dd'T'HH:mm:ssZ"));

	public GitProperties(Properties entries) {
		super(processEntries(entries));
	}

	/**
	 * Return the name of the branch or {@code null}.
	 * @return the branch
	 */
	public String getBranch() {
		return get("branch");
	}

	/**
	 * Return the full id of the commit or {@code null}.
	 * @return the full commit id
	 */
	public String getCommitId() {
		return get("commit.id");
	}

	/**
	 * Return the abbreviated id of the commit or {@code null}.
	 * @return the short commit id
	 */
	public String getShortCommitId() {
		String shortId = get("commit.id.abbrev");
		if (shortId != null) {
			return shortId;
		}
		String id = getCommitId();
		if (id == null) {
			return null;
		}
		return (id.length() > 7) ? id.substring(0, 7) : id;
	}

	/**
	 * Return the timestamp of the commit or {@code null}.
	 * <p>
	 * If the original value could not be parsed properly, it is still available with the
	 * {@code commit.time} key.
	 * @return the commit time
	 * @see #get(String)
	 */
	public Instant getCommitTime() {
		return getInstant("commit.time");
	}

	private static Properties processEntries(Properties properties) {
		coercePropertyToEpoch(properties, "commit.time");
		coercePropertyToEpoch(properties, "build.time");
		Object commitId = properties.get("commit.id");
		if (commitId != null) {
			// Can get converted into a map, so we copy the entry as a nested key
			properties.put("commit.id.full", commitId);
		}
		return properties;
	}

	private static void coercePropertyToEpoch(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value != null) {
			properties.setProperty(key,
					coercers.stream()
						.map((coercer) -> coercer.apply(value))
						.filter(Objects::nonNull)
						.findFirst()
						.orElse(value));
		}
	}

	/**
	 * {@link RuntimeHintsRegistrar} for git properties.
	 */
	static class GitPropertiesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("git.properties");
		}

	}

	/**
	 * Coercer used to convert a source value to epoch time.
	 */
	private record Coercer(Function<String, Long> action, Predicate<RuntimeException> ignoredExceptions) {

		/**
		 * Attempt to convert the specified value to epoch time.
		 * @param value the value to coerce to
		 * @return the epoch time in milliseconds or {@code null}
		 */
		String apply(String value) {
			try {
				Long result = this.action.apply(value);
				return (result != null) ? String.valueOf(result) : null;
			}
			catch (RuntimeException ex) {
				if (this.ignoredExceptions.test(ex)) {
					return null;
				}
				throw ex;
			}
		}

		static Coercer milliseconds() {
			return new Coercer((value) -> Long.parseLong(value) * 1000, NumberFormatException.class::isInstance);
		}

		static Coercer dateTimePattern(String pattern) {
			return new Coercer(
					(value) -> DateTimeFormatter.ofPattern(pattern).parse(value, Instant::from).toEpochMilli(),
					DateTimeParseException.class::isInstance);
		}

	}

}
