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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Sanitizer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chris Bono
 * @author David Good
 * @author Madhura Bhave
 */
class SanitizerTests {

	@Test
	void whenNoSanitizationFunctionAndShowUnsanitizedIsFalse() {
		Sanitizer sanitizer = new Sanitizer();
		assertThat(sanitizer.sanitize(new SanitizableData(null, "password", "secret"), false)).isEqualTo("******");
		assertThat(sanitizer.sanitize(new SanitizableData(null, "other", "something"), false)).isEqualTo("******");
	}

	@Test
	void whenNoSanitizationFunctionAndShowUnsanitizedIsTrue() {
		Sanitizer sanitizer = new Sanitizer();
		assertThat(sanitizer.sanitize(new SanitizableData(null, "password", "secret"), true)).isEqualTo("secret");
		assertThat(sanitizer.sanitize(new SanitizableData(null, "other", "something"), true)).isEqualTo("something");
	}

	@Test
	void whenCustomSanitizationFunctionAndShowUnsanitizedIsFalse() {
		Sanitizer sanitizer = new Sanitizer(Collections.singletonList((data) -> {
			if (data.getKey().equals("custom")) {
				return data.withValue("$$$$$$");
			}
			return data;
		}));
		SanitizableData secret = new SanitizableData(null, "secret", "xyz");
		assertThat(sanitizer.sanitize(secret, false)).isEqualTo("******");
		SanitizableData custom = new SanitizableData(null, "custom", "abcde");
		assertThat(sanitizer.sanitize(custom, false)).isEqualTo("******");
		SanitizableData hello = new SanitizableData(null, "hello", "abc");
		assertThat(sanitizer.sanitize(hello, false)).isEqualTo("******");
	}

	@Test
	void whenCustomSanitizationFunctionAndShowUnsanitizedIsTrue() {
		Sanitizer sanitizer = new Sanitizer(Collections.singletonList((data) -> {
			if (data.getKey().equals("custom")) {
				return data.withValue("$$$$$$");
			}
			return data;
		}));
		SanitizableData secret = new SanitizableData(null, "secret", "xyz");
		assertThat(sanitizer.sanitize(secret, true)).isEqualTo("xyz");
		SanitizableData custom = new SanitizableData(null, "custom", "abcde");
		assertThat(sanitizer.sanitize(custom, true)).isEqualTo("$$$$$$");
		SanitizableData hello = new SanitizableData(null, "hello", "abc");
		assertThat(sanitizer.sanitize(hello, true)).isEqualTo("abc");
	}

	@Test
	void overridingDefaultSanitizingFunction() {
		Sanitizer sanitizer = new Sanitizer(Collections.singletonList((data) -> {
			if (data.getKey().equals("password")) {
				return data.withValue("------");
			}
			return data;
		}));
		SanitizableData password = new SanitizableData(null, "password", "123456");
		assertThat(sanitizer.sanitize(password, true)).isEqualTo("------");
	}

	@Test
	void whenValueSanitizedLaterSanitizingFunctionsShouldBeSkipped() {
		final String sameKey = "custom";
		List<SanitizingFunction> sanitizingFunctions = new ArrayList<>();
		sanitizingFunctions.add((data) -> {
			if (data.getKey().equals(sameKey)) {
				return data.withValue("------");
			}
			return data;
		});
		sanitizingFunctions.add((data) -> {
			if (data.getKey().equals(sameKey)) {
				return data.withValue("******");
			}
			return data;
		});
		Sanitizer sanitizer = new Sanitizer(sanitizingFunctions);
		SanitizableData custom = new SanitizableData(null, sameKey, "123456");
		assertThat(sanitizer.sanitize(custom, true)).isEqualTo("------");
	}

}
