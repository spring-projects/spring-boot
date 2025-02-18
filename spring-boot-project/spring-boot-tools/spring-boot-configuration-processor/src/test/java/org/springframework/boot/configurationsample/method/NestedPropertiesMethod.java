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

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;

@ConfigurationProperties("method-nested")
public class NestedPropertiesMethod {

	private String myProperty;

	private final NestedProperty nested = new NestedProperty();

	private final Inner inner = new Inner();

	public String getMyProperty() {
		return this.myProperty;
	}

	public void setMyProperty(String myProperty) {
		this.myProperty = myProperty;
	}

	@NestedConfigurationProperty
	public NestedProperty getNested() {
		return this.nested;
	}

	public Inner getInner() {
		return this.inner;
	}

	public static class Inner {

		private String myInnerProperty;

		private final NestedProperty nested = new NestedProperty();

		public String getMyInnerProperty() {
			return this.myInnerProperty;
		}

		public void setMyInnerProperty(String myInnerProperty) {
			this.myInnerProperty = myInnerProperty;
		}

		@NestedConfigurationProperty
		public NestedProperty getNested() {
			return this.nested;
		}

	}

}
