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

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

/**
 * Properties with non-static nested classes.
 *
 * @author Daeho Kwon
 */
@TestConfigurationProperties("non-static-inner")
public class NonStaticInnerClassProperties {

	private InnerWithSetter innerWithSetter;

	private final InnerWithoutSetter innerWithoutSetter = new InnerWithoutSetter();

	private InnerWithoutSetter initializedWithoutSetter = new InnerWithoutSetter();

	private InnerWithoutSetter uninitializedWithoutSetter;

	@TestNestedConfigurationProperty
	private InnerWithSetter explicitlyNested;

	public InnerWithSetter getInnerWithSetter() {
		return this.innerWithSetter;
	}

	public void setInnerWithSetter(InnerWithSetter innerWithSetter) {
		this.innerWithSetter = innerWithSetter;
	}

	public InnerWithoutSetter getInnerWithoutSetter() {
		return this.innerWithoutSetter;
	}

	public InnerWithoutSetter getInitializedWithoutSetter() {
		return this.initializedWithoutSetter;
	}

	public InnerWithoutSetter getUninitializedWithoutSetter() {
		return this.uninitializedWithoutSetter;
	}

	public InnerWithSetter getExplicitlyNested() {
		return this.explicitlyNested;
	}

	public void setExplicitlyNested(InnerWithSetter explicitlyNested) {
		this.explicitlyNested = explicitlyNested;
	}

	public class InnerWithSetter {

		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public class InnerWithoutSetter {

		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

}
