/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.virtualan.mapping.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * pet status in the store.
 */
public enum StatusEnum {

	/**
	 * pet status as available.
	 */
	AVAILABLE("available"),

	/**
	 * pet status as pending.
	 */
	PENDING("pending"),

	/**
	 * pet status as sold.
	 */
	SOLD("sold");

	private String value;

	StatusEnum(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(this.value);
	}

	@JsonCreator
	public static StatusEnum fromValue(String text) {
		for (StatusEnum b : StatusEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}

}
