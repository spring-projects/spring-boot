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

package org.springframework.boot.configurationsample.immutable;

/**
 * Simple immutable properties with primitive types.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutablePrimitiveProperties {

	private final boolean flag;

	private final byte octet;

	private final char letter;

	private final short number;

	private final int counter;

	private final long value;

	private final float percentage;

	private final double ratio;

	public ImmutablePrimitiveProperties(boolean flag, byte octet, char letter,
			short number, int counter, long value, float percentage, double ratio) {
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
