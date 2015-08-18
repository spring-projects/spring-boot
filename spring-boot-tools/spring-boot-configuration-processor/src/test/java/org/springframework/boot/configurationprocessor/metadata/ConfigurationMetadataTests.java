/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link ConfigurationMetadata}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataTests {

	@Test
	public void toDashedCaseCamelCase() {
		assertThat(toDashedCase("simpleCamelCase"), is("simple-camel-case"));
	}

	@Test
	public void toDashedCaseWordsUnderScore() {
		assertThat(toDashedCase("Word_With_underscore"), is("word_with_underscore"));
	}

	@Test
	public void toDashedCaseWordsSeveralUnderScores() {
		assertThat(toDashedCase("Word___With__underscore"), is("word___with__underscore"));
	}

	@Test
	public void toDashedCaseLowerCaseUnderscore() {
		assertThat(toDashedCase("lower_underscore"), is("lower_underscore"));
	}

	@Test
	public void toDashedCaseUpperUnderscore() {
		assertThat(toDashedCase("UPPER_UNDERSCORE"), is("upper_underscore"));
	}

	@Test
	public void toDashedCaseMultipleUnderscores() {
		assertThat(toDashedCase("super___crazy"), is("super___crazy"));
	}

	@Test
	public void toDashedCaseUppercase() {
		assertThat(toDashedCase("UPPERCASE"), is("uppercase"));
	}

	@Test
	public void toDashedCaseLowercase() {
		assertThat(toDashedCase("lowercase"), is("lowercase"));
	}

	private String toDashedCase(String name) {
		return ConfigurationMetadata.toDashedCase(name);
	}

}
