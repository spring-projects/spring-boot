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

import org.springframework.boot.configurationsample.MetaConstructorBinding;

/**
 * A type that declares constructor binding but with two available constructors.
 *
 * @author Stephane Nicoll
 */
@MetaConstructorBinding
@SuppressWarnings("unused")
public class TwoConstructorsClassConstructorBindingExample {

	private String name;

	private String description;

	public TwoConstructorsClassConstructorBindingExample(String name) {
		this(name, null);
	}

	public TwoConstructorsClassConstructorBindingExample(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
