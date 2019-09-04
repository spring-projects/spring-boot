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

package org.springframework.boot.context.properties.source;

import java.util.List;

/**
 * Exception thrown when {@link ConfigurationPropertyName} has invalid characters.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class InvalidConfigurationPropertyNameException extends RuntimeException {

	private final CharSequence name;

	private final List<Character> invalidCharacters;

	public InvalidConfigurationPropertyNameException(CharSequence name, List<Character> invalidCharacters) {
		super("Configuration property name '" + name + "' is not valid");
		this.name = name;
		this.invalidCharacters = invalidCharacters;
	}

	public List<Character> getInvalidCharacters() {
		return this.invalidCharacters;
	}

	public CharSequence getName() {
		return this.name;
	}

	public static void throwIfHasInvalidChars(CharSequence name, List<Character> invalidCharacters) {
		if (!invalidCharacters.isEmpty()) {
			throw new InvalidConfigurationPropertyNameException(name, invalidCharacters);
		}
	}

}
