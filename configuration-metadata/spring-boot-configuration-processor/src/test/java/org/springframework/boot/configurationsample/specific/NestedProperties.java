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

package org.springframework.boot.configurationsample.specific;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

/**
 * Demonstrate the auto-detection of config classes which contains nested properties.
 *
 * @author Yanming Zhou
 */
@TestConfigurationProperties("config")
public class NestedProperties {

	@TestNestedConfigurationProperty
	private final Foo foo1 = new Foo();

	@TestNestedConfigurationProperty
	private @Nullable Foo foo2;

	public Foo getFoo1() {
		return this.foo1;
	}

	public @Nullable Foo getFoo2() {
		return this.foo2;
	}

	public void setFoo2(@Nullable Foo foo2) {
		this.foo2 = foo2;
	}

	public static class Foo {

		private boolean enabled = true;

		@TestNestedConfigurationProperty
		private final Bar bar1 = new Bar();

		@TestNestedConfigurationProperty
		private @Nullable Bar bar2;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Bar getBar1() {
			return this.bar1;
		}

		public @Nullable Bar getBar2() {
			return this.bar2;
		}

		public void setBar2(@Nullable Bar bar2) {
			this.bar2 = bar2;
		}

	}

	public static class Bar {

		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
