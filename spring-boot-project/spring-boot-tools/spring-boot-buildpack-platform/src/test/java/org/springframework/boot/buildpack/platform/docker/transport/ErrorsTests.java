/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.transport.Errors.Error;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Errors}.
 *
 * @author Phillip Webb
 */
class ErrorsTests extends AbstractJsonTests {

	@Test
	void readValueDeserializesJson() throws Exception {
		Errors errors = getObjectMapper().readValue(getContent("errors.json"), Errors.class);
		Iterator<Error> iterator = errors.iterator();
		Error error1 = iterator.next();
		Error error2 = iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(error1.getCode()).isEqualTo("TEST1");
		assertThat(error1.getMessage()).isEqualTo("Test One");
		assertThat(error2.getCode()).isEqualTo("TEST2");
		assertThat(error2.getMessage()).isEqualTo("Test Two");
	}

	@Test
	void toStringHasErrorDetails() throws Exception {
		Errors errors = getObjectMapper().readValue(getContent("errors.json"), Errors.class);
		assertThat(errors).hasToString("[TEST1: Test One, TEST2: Test Two]");
	}

}
