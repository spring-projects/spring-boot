/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.ssl;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslOptions}.
 *
 * @author Phillip Webb
 */
class SslOptionsTests {

	@Test
	void noneReturnsNull() {
		SslOptions options = SslOptions.NONE;
		assertThat(options.getCiphers()).isNull();
		assertThat(options.getEnabledProtocols()).isNull();
	}

	@Test
	void ofWithArrayCreatesSslOptions() {
		String[] ciphers = { "a", "b", "c" };
		String[] enabledProtocols = { "d", "e", "f" };
		SslOptions options = SslOptions.of(ciphers, enabledProtocols);
		assertThat(options.getCiphers()).containsExactly(ciphers);
		assertThat(options.getEnabledProtocols()).containsExactly(enabledProtocols);
	}

	@Test
	void ofWithNullArraysCreatesSslOptions() {
		String[] ciphers = null;
		String[] enabledProtocols = null;
		SslOptions options = SslOptions.of(ciphers, enabledProtocols);
		assertThat(options.getCiphers()).isNull();
		assertThat(options.getEnabledProtocols()).isNull();
	}

	@Test
	void ofWithSetCreatesSslOptions() {
		Set<String> ciphers = Set.of("a", "b", "c");
		Set<String> enabledProtocols = Set.of("d", "e", "f");
		SslOptions options = SslOptions.of(ciphers, enabledProtocols);
		assertThat(options.getCiphers()).contains("a", "b", "c");
		assertThat(options.getEnabledProtocols()).contains("d", "e", "f");
	}

	@Test
	void ofWithNullSetCreatesSslOptions() {
		Set<String> ciphers = null;
		Set<String> enabledProtocols = null;
		SslOptions options = SslOptions.of(ciphers, enabledProtocols);
		assertThat(options.getCiphers()).isNull();
		assertThat(options.getEnabledProtocols()).isNull();
	}

	@Test
	void isSpecifiedWhenHasCiphers() {
		SslOptions options = SslOptions.of(Set.of("a", "b", "c"), null);
		assertThat(options.isSpecified()).isTrue();
	}

	@Test
	void isSpecifiedWhenHasEnabledProtocols() {
		SslOptions options = SslOptions.of(null, Set.of("d", "e", "f"));
		assertThat(options.isSpecified()).isTrue();
	}

	@Test
	void isSpecifiedWhenHasNoCiphersOrEnabledProtocols() {
		SslOptions options = SslOptions.NONE;
		assertThat(options.isSpecified()).isFalse();
	}

}
