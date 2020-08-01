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

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;

/**
 * Demonstrate inner classes end up in metadata regardless of position in hierarchy and
 * without the use of {@link NestedConfigurationProperty @NestedConfigurationProperty}.
 *
 * @author Madhura Bhave
 */
@ConfigurationProperties(prefix = "config")
public class InnerClassHierarchicalProperties {

	private Foo foo;

	public Foo getFoo() {
		return this.foo;
	}

	public void setFoo(Foo foo) {
		this.foo = foo;
	}

	public static class Foo {

		private Bar bar;

		public Bar getBar() {
			return this.bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

		public static class Baz {

			private String blah;

			public String getBlah() {
				return this.blah;
			}

			public void setBlah(String blah) {
				this.blah = blah;
			}

		}

	}

	public static class Bar {

		private String bling;

		private Foo.Baz baz;

		public String getBling() {
			return this.bling;
		}

		public void setBling(String foo) {
			this.bling = foo;
		}

		public Foo.Baz getBaz() {
			return this.baz;
		}

		public void setBaz(Foo.Baz baz) {
			this.baz = baz;
		}

	}

}
