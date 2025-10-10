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

package org.springframework.boot.context.properties.bind;

import java.util.Collections;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to ensure that the {@link Binder} offers at least some support for
 * Boot 1.5 style binding.
 *
 * @author Phillip Webb
 */
class BackCompatibilityBinderIntegrationTests {

	@Test
	void bindWhenBindingCamelCaseToEnvironmentWithExtractUnderscore() {
		// gh-10873
		MockEnvironment environment = new MockEnvironment();
		SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("FOO_ZK_NODES", "foo"));
		environment.getPropertySources().addFirst(propertySource);
		ExampleCamelCaseBean result = Binder.get(environment)
			.bind("foo", Bindable.of(ExampleCamelCaseBean.class))
			.get();
		assertThat(result.getZkNodes()).isEqualTo("foo");
	}

	@Test
	void bindWhenUsingSystemEnvironmentToOverride() {
		MockEnvironment environment = new MockEnvironment();
		SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource("override",
				Collections.singletonMap("foo.password", "test"));
		environment.getPropertySources().addFirst(propertySource);
		PasswordProperties result = Binder.get(environment).bind("foo", Bindable.of(PasswordProperties.class)).get();
		assertThat(result.getPassword()).isEqualTo("test");
	}

	static class ExampleCamelCaseBean {

		private @Nullable String zkNodes;

		@Nullable String getZkNodes() {
			return this.zkNodes;
		}

		void setZkNodes(@Nullable String zkNodes) {
			this.zkNodes = zkNodes;
		}

	}

	static class PasswordProperties {

		private @Nullable String password;

		@Nullable String getPassword() {
			return this.password;
		}

		void setPassword(@Nullable String password) {
			this.password = password;
		}

	}

}
