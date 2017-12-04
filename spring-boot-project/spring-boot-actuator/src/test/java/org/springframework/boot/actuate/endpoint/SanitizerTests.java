/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.sanitize.Sanitizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Sanitizer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Piotr Betkier
 */
public class SanitizerTests {

	@Test
	public void defaults() throws Exception {
		Sanitizer<String> sanitizer = Sanitizer.keyBlacklistSanitizer();
		assertThat(sanitizer.sanitize("password", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("my-password", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("my-OTHER.paSSword", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("somesecret", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("somekey", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("token", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("sometoken", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("find", "secret")).isEqualTo("secret");
	}

	@Test
	public void regex() throws Exception {
		Sanitizer<String> sanitizer = Sanitizer.keyBlacklistSanitizer(".*lock.*");
		assertThat(sanitizer.sanitize("verylOCkish", "secret")).isEqualTo("******");
		assertThat(sanitizer.sanitize("veryokish", "secret")).isEqualTo("secret");
	}

	@Test
	public void customFilter() throws Exception {
		Sanitizer.Filter<CustomSource> secretSourceFilter = s -> s.data
				.contains("secret");
		Sanitizer<CustomSource> sanitizer = Sanitizer
				.customRulesSanitizer(Collections.singleton(secretSourceFilter));

		assertThat(sanitizer.sanitize(new CustomSource("secretSource"),
				"should-be-sanitized")).isEqualTo("******");
		assertThat(sanitizer.sanitize(new CustomSource("ordinarySource"),
				"should-not-be-sanitized")).isEqualTo("should-not-be-sanitized");
	}

	private static final class CustomSource {

		private final String data;

		private CustomSource(String data) {
			this.data = data;
		}
	}
}
