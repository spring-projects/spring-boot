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
			@TestDefaultValue("120") Byte octet, @TestDefaultValue("a") Character letter, @TestDefaultValue("1000") Short number,
			@TestDefaultValue("42") Integer counter, @TestDefaultValue("2000") Long value,
			@TestDefaultValue("0.5") Float percentage, @TestDefaultValue("42.42") Double ratio) {
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
