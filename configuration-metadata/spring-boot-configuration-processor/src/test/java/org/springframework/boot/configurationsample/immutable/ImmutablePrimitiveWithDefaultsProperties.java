/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Simple immutable properties with primitive types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutablePrimitiveWithDefaultsProperties {

	private final boolean flag;

	private final byte octet;

	private final char letter;

	private final short number;

	private final int counter;

	private final long value;

	private final float percentage;

	private final double ratio;

	public ImmutablePrimitiveWithDefaultsProperties(@TestDefaultValue("true") boolean flag,
			@TestDefaultValue("120") byte octet, @TestDefaultValue("a") char letter,
			@TestDefaultValue("1000") short number, @TestDefaultValue("42") int counter,
			@TestDefaultValue("2000") long value, @TestDefaultValue("0.5") float percentage,
			@TestDefaultValue("42.42") double ratio) {
		this.flag = flag;
		this.octet = octet;
		this.letter = letter;
		this.number = number;
		this.counter = counter;
		this.value = value;
		this.percentage = percentage;
		this.ratio = ratio;
	}

}
