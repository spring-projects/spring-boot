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

package org.springframework.boot.configurationsample.source;

/**
 * Immutable type with manual metadata. This illustrates the case where the type of a
 * property is defined in a separate class and source-based metadata cannot be discovered.
 */
public class ImmutableSource {

	@SuppressWarnings("unused")
	private final String name;

	@SuppressWarnings("unused")
	private final String description;

	@SuppressWarnings("unused")
	private final String type;

	public ImmutableSource(String name, String description, String type) {
		this.name = name;
		this.description = description;
		this.type = type;
	}

}
