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

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Nested properties with a private constructor.
 *
 * @author Phillip Webb
 */
@TestConfigurationProperties("config")
public class InnerClassWithPrivateConstructor {

	private Nested nested = new Nested("whatever");

	public Nested getNested() {
		return this.nested;
	}

	public void setNested(Nested nested) {
		this.nested = nested;
	}

	public static final class Nested {

		private String name;

		private Nested(String ignored) {
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
