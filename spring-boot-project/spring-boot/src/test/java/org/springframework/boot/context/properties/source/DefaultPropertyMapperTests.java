/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DefaultPropertyMapperTests extends AbstractPropertyMapperTests {

	@Override
	protected PropertyMapper getMapper() {
		return DefaultPropertyMapper.INSTANCE;
	}

	@Test
	void mapFromStringShouldReturnBestGuess() {
		assertThat(namesFromString("server")).toIterable().containsExactly("server");
		assertThat(namesFromString("server.port")).toIterable().containsExactly("server.port");
		assertThat(namesFromString("host[0]")).toIterable().containsExactly("host[0]");
		assertThat(namesFromString("host[0][1]")).toIterable().containsExactly("host[0][1]");
		assertThat(namesFromString("host[0].name")).toIterable().containsExactly("host[0].name");
		assertThat(namesFromString("host.f00.name")).toIterable().containsExactly("host.f00.name");
		assertThat(namesFromString("my.host-name")).toIterable().containsExactly("my.host-name");
		assertThat(namesFromString("my.hostName")).toIterable().containsExactly("my.hostname");
		assertThat(namesFromString("my.HOST_NAME")).toIterable().containsExactly("my.hostname");
		assertThat(namesFromString("s[!@#$%^&*()=+]e-rVeR")).toIterable().containsExactly("s[!@#$%^&*()=+].e-rver");
		assertThat(namesFromString("host[FOO].name")).toIterable().containsExactly("host[FOO].name");
	}

	@Test
	void mapFromConfigurationShouldReturnBestGuess() {
		assertThat(namesFromConfiguration("server")).toIterable().containsExactly("server");
		assertThat(namesFromConfiguration("server.port")).toIterable().containsExactly("server.port");
		assertThat(namesFromConfiguration("host[0]")).toIterable().containsExactly("host[0]");
		assertThat(namesFromConfiguration("host[0][1]")).toIterable().containsExactly("host[0][1]");
		assertThat(namesFromConfiguration("host[0].name")).toIterable().containsExactly("host[0].name");
		assertThat(namesFromConfiguration("host.f00.name")).toIterable().containsExactly("host.f00.name");
		assertThat(namesFromConfiguration("my.host-name")).toIterable().containsExactly("my.host-name");
		assertThat(namesFromConfiguration("host[FOO].name")).toIterable().containsExactly("host[FOO].name");
	}

}
