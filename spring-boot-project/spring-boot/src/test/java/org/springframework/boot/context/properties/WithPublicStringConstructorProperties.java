/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.properties;

/**
 * A {@link ConfigurationProperties @ConfigurationProperties} with an additional
 * single-arg public constructor, along with layered inner classes. Used in
 * {@link ConfigurationPropertiesTests}.
 *
 * @author Madhura Bhave
 * @author prasoonanand
 */
@ConfigurationProperties(prefix = "test")
public class WithPublicStringConstructorProperties {

	private String a;

	private Bar bar;

	public WithPublicStringConstructorProperties() {
	}

	public WithPublicStringConstructorProperties(String a) {
		this.a = a;
	}

	public String getA() {
		return this.a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public Bar getBar() {
		return this.bar;
	}

	public void setBar(Bar bar) {
		this.bar = bar;
	}

	public static class Bar {

		private String baz;

		private Foo foo;

		public Bar() {
		}

		public Bar(String baz) {
			this.baz = baz;
		}

		public String getBaz() {
			return this.baz;
		}

		public void setBaz(String baz) {
			this.baz = baz;
		}

		public Foo getFoo() {
			return this.foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}

		public static class Foo {

			private String a;

			public Foo() {
			}

			public Foo(String a) {
				this.a = a;
			}

			public String getA() {
				return this.a;
			}

			public void setA(String a) {
				this.a = a;
			}

		}

	}

}
