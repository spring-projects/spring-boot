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

package org.springframework.boot.context.properties.bind;

import java.util.Collections;

import org.junit.Test;

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
public class BackCompatibilityBinderIntegrationTests {

	@Test
	public void bindWhenBindingCamelCaseToEnvironmentWithExtractUnderscore() {
		// gh-10873
		MockEnvironment environment = new MockEnvironment();
		SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("FOO_ZK_NODES", "foo"));
		environment.getPropertySources().addFirst(propertySource);
		ExampleCamelCaseBean result = Binder.get(environment)
				.bind("foo", Bindable.of(ExampleCamelCaseBean.class)).get();
		assertThat(result.getZkNodes()).isEqualTo("foo");
	}

	@Test
	public void bindWhenUsingSystemEnvironmentToOverride() {
		MockEnvironment environment = new MockEnvironment();
		SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource(
				"override", Collections.singletonMap("foo.password", "test"));
		environment.getPropertySources().addFirst(propertySource);
		PasswordProperties result = Binder.get(environment)
				.bind("foo", Bindable.of(PasswordProperties.class)).get();
		assertThat(result.getPassword()).isEqualTo("test");
	}

	public static class ExampleCamelCaseBean {

		private String zkNodes;

		public String getZkNodes() {
			return this.zkNodes;
		}

		public void setZkNodes(String zkNodes) {
			this.zkNodes = zkNodes;
		}

	}

	protected static class PasswordProperties {

		private String password;

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}
}
