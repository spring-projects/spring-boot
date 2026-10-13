/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestConstructorBinding;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Demonstrates that an invalid default character value leads to a compilation failure.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("test")
public class InvalidDefaultValueCharacterProperties {

	private final char letter;

	@TestConstructorBinding
	public InvalidDefaultValueCharacterProperties(@TestDefaultValue("bad") char letter) {
		this.letter = letter;
	}

	public char getLetter() {
		return this.letter;
	}

}
