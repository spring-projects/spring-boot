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

package org.springframework.boot.actuate.endpoint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link Sanitizer}.
 *
 * @author Phillip Webb
 */
public class SanitizerTests {

	private Sanitizer sanitizer = new Sanitizer();

	@Test
	public void defaults() throws Exception {
		assertEquals(this.sanitizer.sanitize("password", "secret"), "******");
		assertEquals(this.sanitizer.sanitize("my-password", "secret"), "******");
		assertEquals(this.sanitizer.sanitize("my-OTHER.paSSword", "secret"), "******");
		assertEquals(this.sanitizer.sanitize("somesecret", "secret"), "******");
		assertEquals(this.sanitizer.sanitize("somekey", "secret"), "******");
		assertEquals(this.sanitizer.sanitize("find", "secret"), "secret");
	}

	@Test
	public void regex() throws Exception {
		this.sanitizer.setKeysToSanitize(".*lock.*");
		assertEquals(this.sanitizer.sanitize("verylOCkish", "secret"), "******");
		assertEquals(this.sanitizer.sanitize("veryokish", "secret"), "secret");
	}

}
