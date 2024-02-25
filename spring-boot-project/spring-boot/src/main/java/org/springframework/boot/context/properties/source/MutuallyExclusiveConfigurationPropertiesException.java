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

package org.springframework.boot.context.properties.source;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * Exception thrown when more than one mutually exclusive configuration property has been
 * configured.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
@SuppressWarnings("serial")
public class MutuallyExclusiveConfigurationPropertiesException extends RuntimeException {

	private final Set<String> configuredNames;

	private final Set<String> mutuallyExclusiveNames;

	/**
	 * Creates a new instance for mutually exclusive configuration properties when two or
	 * more of those properties have been configured.
	 * @param configuredNames the names of the properties that have been configured
	 * @param mutuallyExclusiveNames the names of the properties that are mutually
	 * exclusive
	 */
	public MutuallyExclusiveConfigurationPropertiesException(Collection<String> configuredNames,
			Collection<String> mutuallyExclusiveNames) {
		this(asSet(configuredNames), asSet(mutuallyExclusiveNames));
	}

	/**
	 * Constructs a new MutuallyExclusiveConfigurationPropertiesException with the
	 * specified configured names and mutually exclusive names.
	 * @param configuredNames the set of configured property names
	 * @param mutuallyExclusiveNames the set of mutually exclusive property names
	 */
	private MutuallyExclusiveConfigurationPropertiesException(Set<String> configuredNames,
			Set<String> mutuallyExclusiveNames) {
		super(buildMessage(mutuallyExclusiveNames, configuredNames));
		this.configuredNames = configuredNames;
		this.mutuallyExclusiveNames = mutuallyExclusiveNames;
	}

	/**
	 * Return the names of the properties that have been configured.
	 * @return the names of the configured properties
	 */
	public Set<String> getConfiguredNames() {
		return this.configuredNames;
	}

	/**
	 * Return the names of the properties that are mutually exclusive.
	 * @return the names of the mutually exclusive properties
	 */
	public Set<String> getMutuallyExclusiveNames() {
		return this.mutuallyExclusiveNames;
	}

	/**
	 * Converts a collection of strings into a set of strings.
	 * @param collection the collection of strings to be converted
	 * @return a set of strings containing the elements from the collection, or null if
	 * the collection is null
	 */
	private static Set<String> asSet(Collection<String> collection) {
		return (collection != null) ? new LinkedHashSet<>(collection) : null;
	}

	/**
	 * Builds a message for the MutuallyExclusiveConfigurationPropertiesException.
	 * @param mutuallyExclusiveNames the set of mutually exclusive names
	 * @param configuredNames the set of configured names
	 * @return the message indicating the mutually exclusive and configured names
	 * @throws IllegalArgumentException if configuredNames or mutuallyExclusiveNames is
	 * null or contains less than 2 names
	 */
	private static String buildMessage(Set<String> mutuallyExclusiveNames, Set<String> configuredNames) {
		Assert.isTrue(configuredNames != null && configuredNames.size() > 1,
				"ConfiguredNames must contain 2 or more names");
		Assert.isTrue(mutuallyExclusiveNames != null && mutuallyExclusiveNames.size() > 1,
				"MutuallyExclusiveNames must contain 2 or more names");
		return "The configuration properties '" + String.join(", ", mutuallyExclusiveNames)
				+ "' are mutually exclusive and '" + String.join(", ", configuredNames)
				+ "' have been configured together";
	}

	/**
	 * Throw a new {@link MutuallyExclusiveConfigurationPropertiesException} if multiple
	 * non-null values are defined in a set of entries.
	 * @param entries a consumer used to populate the entries to check
	 */
	public static void throwIfMultipleNonNullValuesIn(Consumer<Map<String, Object>> entries) {
		Map<String, Object> map = new LinkedHashMap<>();
		entries.accept(map);
		Set<String> configuredNames = map.entrySet()
			.stream()
			.filter((entry) -> entry.getValue() != null)
			.map(Map.Entry::getKey)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		if (configuredNames.size() > 1) {
			throw new MutuallyExclusiveConfigurationPropertiesException(configuredNames, map.keySet());
		}
	}

}
