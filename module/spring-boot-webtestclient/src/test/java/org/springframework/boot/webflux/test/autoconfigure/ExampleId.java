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

package org.springframework.boot.webflux.test.autoconfigure;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;

/**
 * An example attribute that requires a {@link Converter}.
 *
 * @author Stephane Nicoll
 */
public class ExampleId {

	private final UUID id;

	ExampleId(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return this.id;
	}

}
