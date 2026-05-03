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

package org.springframework.boot.loader.net.protocol.jar;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileUrlKey}.
 *
 * @author Phillip Webb
 */
class JarFileUrlKeyTests {

	@BeforeAll
	static void setup() {
		Handlers.register();
	}

	@Test
	void equalsAndHashCode() throws Exception {
		JarFileUrlKey k1 = key("jar:nested:/my.jar/!mynested.jar!/my/path");
		JarFileUrlKey k2 = key("jar:nested:/my.jar/!mynested.jar!/my/path");
		JarFileUrlKey k3 = key("jar:nested:/my.jar/!mynested.jar!/my/path2");
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode())
			.isEqualTo("nested:/my.jar/!mynested.jar!/my/path".hashCode());
		assertThat(k1).isEqualTo(k1).isEqualTo(k2).isNotEqualTo(k3);
	}

	@Test
	void equalsWhenUppercaseAndLowercaseProtocol() throws Exception {
		JarFileUrlKey k1 = key("JAR:nested:/my.jar/!mynested.jar!/my/path");
		JarFileUrlKey k2 = key("jar:nested:/my.jar/!mynested.jar!/my/path");
		assertThat(k1).isEqualTo(k2);
	}

	@Test
	void equalsWhenHasHostAndPort() throws Exception {
		JarFileUrlKey k1 = key("https://example.com:1234/test");
		JarFileUrlKey k2 = key("https://example.com:1234/test");
		assertThat(k1).isEqualTo(k2);
	}

	@Test
	void equalsWhenHasUppercaseAndLowercaseHost() throws Exception {
		JarFileUrlKey k1 = key("https://EXAMPLE.com:1234/test");
		JarFileUrlKey k2 = key("https://example.com:1234/test");
		assertThat(k1).isEqualTo(k2);
	}

	@Test
	void equalsWhenHasNoPortUsesDefaultPort() throws Exception {
		JarFileUrlKey k1 = key("https://EXAMPLE.com/test");
		JarFileUrlKey k2 = key("https://example.com:443/test");
		assertThat(k1).isEqualTo(k2);
	}

	@Test
	void equalsWhenHasNoFile() throws Exception {
		JarFileUrlKey k1 = key("https://EXAMPLE.com");
		JarFileUrlKey k2 = key("https://example.com:443");
		assertThat(k1).isEqualTo(k2);
	}

	@Test
	void equalsWhenHasRuntimeRef() throws Exception {
		JarFileUrlKey k1 = key("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
		JarFileUrlKey k2 = key("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
		assertThat(k1).isEqualTo(k2);
	}

	@Test
	void equalsWhenHasOtherRefIgnoresRefs() throws Exception {
		JarFileUrlKey k1 = key("jar:nested:/my.jar/!mynested.jar!/my/path#example");
		JarFileUrlKey k2 = key("jar:nested:/my.jar/!mynested.jar!/my/path");
		assertThat(k1).isEqualTo(k2);
	}

	private JarFileUrlKey key(String spec) throws MalformedURLException {
		return new JarFileUrlKey(new URL(spec));
	}

}
