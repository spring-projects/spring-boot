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

package org.springframework.boot.context.properties.source;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class DefaultPropertyMapperTests extends AbstractPropertyMapperTests {

	@Override
	protected PropertyMapper getMapper() {
		return DefaultPropertyMapper.INSTANCE;
	}

	@Test
	public void mapFromStringShouldReturnBestGuess() {
		assertThat(namesFromString("server")).containsExactly("server");
		assertThat(namesFromString("server.port")).containsExactly("server.port");
		assertThat(namesFromString("host[0]")).containsExactly("host[0]");
		assertThat(namesFromString("host[0][1]")).containsExactly("host[0][1]");
		assertThat(namesFromString("host[0].name")).containsExactly("host[0].name");
		assertThat(namesFromString("host.f00.name")).containsExactly("host.f00.name");
		assertThat(namesFromString("my.host-name")).containsExactly("my.host-name");
		assertThat(namesFromString("my.hostName")).containsExactly("my.hostname");
		assertThat(namesFromString("my.HOST_NAME")).containsExactly("my.hostname");
		assertThat(namesFromString("s[!@#$%^&*()=+]e-rVeR"))
				.containsExactly("s[!@#$%^&*()=+].e-rver");
		assertThat(namesFromString("host[FOO].name")).containsExactly("host[FOO].name");
	}

	@Test
	public void mapFromConfigurationShouldReturnBestGuess() {
		assertThat(namesFromConfiguration("server")).containsExactly("server");
		assertThat(namesFromConfiguration("server.port")).containsExactly("server.port");
		assertThat(namesFromConfiguration("host[0]")).containsExactly("host[0]");
		assertThat(namesFromConfiguration("host[0][1]")).containsExactly("host[0][1]");
		assertThat(namesFromConfiguration("host[0].name"))
				.containsExactly("host[0].name");
		assertThat(namesFromConfiguration("host.f00.name"))
				.containsExactly("host.f00.name");
		assertThat(namesFromConfiguration("my.host-name"))
				.containsExactly("my.host-name");
		assertThat(namesFromConfiguration("host[FOO].name"))
				.containsExactly("host[FOO].name");
	}

}
