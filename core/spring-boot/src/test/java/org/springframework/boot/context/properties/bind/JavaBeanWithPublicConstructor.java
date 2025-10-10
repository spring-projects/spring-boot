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

import org.jspecify.annotations.Nullable;

/**
 * Java bean with an additional public single-arg constructor.
 *
 * @author Phillip Webb
 */
public class JavaBeanWithPublicConstructor {

	private @Nullable String value;

	public JavaBeanWithPublicConstructor() {
	}

	public JavaBeanWithPublicConstructor(String value) {
		setValue(value);
	}

	public @Nullable String getValue() {
		return this.value;
	}

	public void setValue(@Nullable String value) {
		this.value = value;
	}

}
