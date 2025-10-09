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

import org.springframework.boot.configurationsample.TestConstructorBinding;

/**
 * Simple immutable properties with several constructors.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableMultiConstructorProperties {

	private final String name;

	/**
	 * Test description.
	 */
	private final String description;

	public ImmutableMultiConstructorProperties(String name) {
		this(name, null);
	}

	@TestConstructorBinding
	public ImmutableMultiConstructorProperties(String name, String description) {
		this.name = name;
		this.description = description;
	}

}
