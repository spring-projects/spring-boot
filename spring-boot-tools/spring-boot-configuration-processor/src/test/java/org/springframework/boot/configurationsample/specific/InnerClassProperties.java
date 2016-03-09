/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;

/**
 * Demonstrate the auto-detection of inner config classes.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "config")
public class InnerClassProperties {

	private final Foo first = new Foo();

	private Foo second = new Foo();

	@NestedConfigurationProperty
	private final SimplePojo third = new SimplePojo();

	private Fourth fourth;

	public Foo getFirst() {
		return this.first;
	}

	public Foo getTheSecond() {
		return this.second;
	}

	public void setTheSecond(Foo second) {
		this.second = second;
	}

	public SimplePojo getThird() {
		return this.third;
	}

	public Fourth getFourth() {
		return this.fourth;
	}

	public void setFourth(Fourth fourth) {
		this.fourth = fourth;
	}

	public static class Foo {

		private String name;

		private final Bar bar = new Bar();

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bar getBar() {
			return this.bar;
		}

		public static class Bar {

			private String name;

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}

	}

	public enum Fourth {
		YES, NO
	}

}
