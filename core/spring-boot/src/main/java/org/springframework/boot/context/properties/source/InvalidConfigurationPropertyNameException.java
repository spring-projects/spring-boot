/*
 * Copyright 2012-present the original author or authors.
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
