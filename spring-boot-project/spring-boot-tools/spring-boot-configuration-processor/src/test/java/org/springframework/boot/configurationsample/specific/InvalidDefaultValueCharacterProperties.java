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

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestConstructorBinding;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Demonstrates that an invalid default character value leads to a compilation failure.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("test")
public class InvalidDefaultValueCharacterProperties {

	private final char letter;

	@TestConstructorBinding
	public InvalidDefaultValueCharacterProperties(@TestDefaultValue("bad") char letter) {
		this.letter = letter;
	}

	public char getLetter() {
		return this.letter;
	}

}
