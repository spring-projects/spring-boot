/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Simple immutable properties with primitive wrapper types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutablePrimitiveWrapperWithDefaultsProperties {

	private final Boolean flag;

	private final Byte octet;

	private final Character letter;

	private final Short number;

	private final Integer counter;

	private final Long value;

	private final Float percentage;

	private final Double ratio;

	public ImmutablePrimitiveWrapperWithDefaultsProperties(@TestDefaultValue("true") Boolean flag,
			@TestDefaultValue("120") Byte octet, @TestDefaultValue("a") Character letter,
			@TestDefaultValue("1000") Short number, @TestDefaultValue("42") Integer counter,
			@TestDefaultValue("2000") Long value, @TestDefaultValue("0.5") Float percentage,
			@TestDefaultValue("42.42") Double ratio) {
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
