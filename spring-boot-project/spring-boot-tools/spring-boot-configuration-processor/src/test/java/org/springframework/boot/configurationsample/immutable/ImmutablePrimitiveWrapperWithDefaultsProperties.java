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

import org.springframework.boot.configurationsample.DefaultValue;

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

	public ImmutablePrimitiveWrapperWithDefaultsProperties(
			@DefaultValue("true") Boolean flag, @DefaultValue("120") Byte octet,
			@DefaultValue("a") Character letter, @DefaultValue("1000") Short number,
			@DefaultValue("42") Integer counter, @DefaultValue("2000") Long value,
			@DefaultValue("0.5") Float percentage, @DefaultValue("42.42") Double ratio) {
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
