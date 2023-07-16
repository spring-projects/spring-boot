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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthParameterUtils}.
 *
 * @author Alexander Preuß
 */
public class AuthParameterUtilsTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("encodedParamStringConversionProvider")
	void encodedParamStringConversion(String testName, Map<String, String> authParamsMap) {
		String encodedAuthParamString = AuthParameterUtils.maybeConvertToEncodedParamString(authParamsMap);
		if (authParamsMap == null || authParamsMap.isEmpty()) {
			assertThat(encodedAuthParamString).isNull();
		}
		else {
			assertThat(encodedAuthParamString).isEqualTo("{\"audience\":\"urn:sn:pulsar:abc:xyz\","
					+ "\"issuerUrl\":\"https://auth.server.cloud\",\"privateKey\":\"file://Users/xyz/key.json\"}");
		}
	}

	private static Stream<Arguments> encodedParamStringConversionProvider() {
		return Stream.of(Arguments.arguments("null", null), Arguments.arguments("empty", Collections.emptyMap()),
				Arguments.arguments("camelCase",
						Map.of("issuerUrl", "https://auth.server.cloud", "privateKey", "file://Users/xyz/key.json",
								"audience", "urn:sn:pulsar:abc:xyz")),
				Arguments.arguments("kebabCase",
						Map.of("issuer-url", "https://auth.server.cloud", "private-key", "file://Users/xyz/key.json",
								"audience", "urn:sn:pulsar:abc:xyz")),
				Arguments.arguments("lowerCase",
						Map.of("issuerurl", "https://auth.server.cloud", "privatekey", "file://Users/xyz/key.json",
								"audience", "urn:sn:pulsar:abc:xyz")),
				Arguments.arguments("mixed", Map.of("issuerurl", "https://auth.server.cloud", "private-key",
						"file://Users/xyz/key.json", "audience", "urn:sn:pulsar:abc:xyz")));
	}

}
