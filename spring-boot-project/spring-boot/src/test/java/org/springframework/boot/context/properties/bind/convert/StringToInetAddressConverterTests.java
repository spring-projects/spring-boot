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

package org.springframework.boot.context.properties.bind.convert;

import java.net.InetAddress;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToInetAddressConverter}.
 *
 * @author Phillip Webb
 */
public class StringToInetAddressConverterTests extends AbstractInetAddressTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StringToInetAddressConverter converter = new StringToInetAddressConverter();

	@Test
	public void convertWhenHostDoesNotExistShouldThrowException() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unknown host");
		this.converter.convert("ireallydontexist.example.com");
	}

	@Test
	public void convertWhenHostExistsShouldConvert() throws Exception {
		assumeResolves("example.com");
		InetAddress converted = this.converter.convert("example.com");
		assertThat(converted.toString()).startsWith("example.com");
	}

}
