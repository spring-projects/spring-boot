/*
 * Copyright 2012-present the original author or authors.
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
