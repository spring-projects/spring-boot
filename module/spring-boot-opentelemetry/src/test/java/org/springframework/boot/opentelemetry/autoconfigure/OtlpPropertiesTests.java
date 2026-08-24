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

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtlpProperties}.
 *
 * @author Moritz Halbritter
 */
class OtlpPropertiesTests {

	private final OtlpProperties properties = new OtlpProperties();

	@Test
	void shouldUseSignalEndpointWhenSet() {
		this.properties.setEndpoint("http://common:4318");
		assertThat(this.properties.resolveEndpoint("http://signal:4318/custom", "v1/traces"))
			.isEqualTo("http://signal:4318/custom");
	}

	@Test
	void shouldFallBackToCommonEndpointWithPathWhenSignalEndpointIsNotSet() {
		this.properties.setEndpoint("http://common:4318");
		assertThat(this.properties.resolveEndpoint(null, "v1/traces")).isEqualTo("http://common:4318/v1/traces");
	}

	@Test
	void shouldFallBackToCommonEndpointWithPathWhenSignalEndpointIsEmpty() {
		this.properties.setEndpoint("http://common:4318");
		assertThat(this.properties.resolveEndpoint("", "v1/traces")).isEqualTo("http://common:4318/v1/traces");
	}

	@Test
	void shouldNotDuplicateSlashWhenCommonEndpointHasTrailingSlash() {
		this.properties.setEndpoint("http://common:4318/");
		assertThat(this.properties.resolveEndpoint(null, "v1/traces")).isEqualTo("http://common:4318/v1/traces");
	}

	@Test
	void shouldReturnCommonEndpointUnchangedWhenPathIsNull() {
		this.properties.setEndpoint("http://common:4318");
		assertThat(this.properties.resolveEndpoint(null, null)).isEqualTo("http://common:4318");
	}

	@Test
	void shouldReturnNullWhenNeitherEndpointIsSet() {
		assertThat(this.properties.resolveEndpoint(null, "v1/traces")).isNull();
	}

	@Test
	void shouldMergeCommonAndSignalHeaders() {
		this.properties.getHeaders().put("common-header", "common-value");
		assertThat(this.properties.mergeHeaders(Map.of("signal-header", "signal-value")))
			.containsEntry("common-header", "common-value")
			.containsEntry("signal-header", "signal-value");
	}

	@Test
	void shouldPreferSignalHeaderWhenKeyIsPresentInBoth() {
		this.properties.getHeaders().put("shared-header", "common-value");
		assertThat(this.properties.mergeHeaders(Map.of("shared-header", "signal-value"))).containsEntry("shared-header",
				"signal-value");
	}

	@Test
	void shouldReturnSignalHeadersWhenNoCommonHeadersAreSet() {
		assertThat(this.properties.mergeHeaders(Map.of("signal-header", "signal-value")))
			.containsExactly(Map.entry("signal-header", "signal-value"));
	}

	@Test
	void shouldUseSignalCompressionWhenSet() {
		this.properties.setCompression(OtlpProperties.Compression.GZIP);
		assertThat(this.properties.resolveCompression(TestCompression.NONE, TestCompression.GZIP, this::map))
			.isEqualTo(TestCompression.NONE);
	}

	@Test
	void shouldFallBackToMappedCommonCompressionWhenSignalCompressionIsNotSet() {
		this.properties.setCompression(OtlpProperties.Compression.GZIP);
		assertThat(this.properties.resolveCompression(null, TestCompression.NONE, this::map))
			.isEqualTo(TestCompression.GZIP);
	}

	@Test
	void shouldReturnDefaultCompressionWhenNeitherIsSet() {
		assertThat(this.properties.resolveCompression(null, TestCompression.NONE, this::map))
			.isEqualTo(TestCompression.NONE);
	}

	private TestCompression map(OtlpProperties.Compression compression) {
		return switch (compression) {
			case GZIP -> TestCompression.GZIP;
			case NONE -> TestCompression.NONE;
		};
	}

	private enum TestCompression {

		GZIP, NONE

	}

}
