/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
	public void toDashedCaseUpperCamelCaseSuffix() {
		assertThat(toDashedCase("myDLQ"), is("my-d-l-q"));
	}

	@Test
	public void toDashedCaseUpperCamelCaseMiddle() {
		assertThat(toDashedCase("someDLQKey"), is("some-d-l-q-key"));
	}

	@Test
	public void toDashedCaseWordsUnderscore() {
		assertThat(toDashedCase("Word_With_underscore"), is("word-with-underscore"));
	}

	@Test
	public void toDashedCaseWordsSeveralUnderscores() {
		assertThat(toDashedCase("Word___With__underscore"),
				is("word---with--underscore"));
	}

	@Test
	public void toDashedCaseLowerCaseUnderscore() {
		assertThat(toDashedCase("lower_underscore"), is("lower-underscore"));
	}

	@Test
	public void toDashedCaseUpperUnderscoreSuffix() {
		assertThat(toDashedCase("my_DLQ"), is("my-d-l-q"));
	}

	@Test
	public void toDashedCaseUpperUnderscoreMiddle() {
		assertThat(toDashedCase("some_DLQ_key"), is("some-d-l-q-key"));
	}

	@Test
	public void toDashedCaseMultipleUnderscores() {
		assertThat(toDashedCase("super___crazy"), is("super---crazy"));
	}

	@Test
	public void toDashedCaseLowercase() {
		assertThat(toDashedCase("lowercase"), is("lowercase"));
	}

	private String toDashedCase(String name) {
		return ConfigurationMetadata.toDashedCase(name);
	}

}
