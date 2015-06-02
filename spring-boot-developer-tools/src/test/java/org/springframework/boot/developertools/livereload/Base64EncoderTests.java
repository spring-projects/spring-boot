/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.livereload;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Base64Encoder}.
 *
 * @author Phillip Webb
 */
public class Base64EncoderTests {

	private static final String TEXT = "Man is distinguished, not only by his reason, "
			+ "but by this singular passion from other animals, which is a lust of the "
			+ "mind, that by a perseverance of delight in the continued and indefatigable "
			+ "generation of knowledge, exceeds the short vehemence of any carnal pleasure.";

	private static final String ENCODED = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5I"
			+ "GhpcyByZWFzb24sIGJ1dCBieSB0aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbm"
			+ "ltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2YgdGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmF"
			+ "uY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGludWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVy"
			+ "YXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRoZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55I"
			+ "GNhcm5hbCBwbGVhc3VyZS4=";

	@Test
	public void encodeText() {
		assertThat(Base64Encoder.encode(TEXT), equalTo(ENCODED));
		assertThat(Base64Encoder.encode("pleasure."), equalTo("cGxlYXN1cmUu"));
		assertThat(Base64Encoder.encode("leasure."), equalTo("bGVhc3VyZS4="));
		assertThat(Base64Encoder.encode("easure."), equalTo("ZWFzdXJlLg=="));
		assertThat(Base64Encoder.encode("asure."), equalTo("YXN1cmUu"));
		assertThat(Base64Encoder.encode("sure."), equalTo("c3VyZS4="));
	}

}
