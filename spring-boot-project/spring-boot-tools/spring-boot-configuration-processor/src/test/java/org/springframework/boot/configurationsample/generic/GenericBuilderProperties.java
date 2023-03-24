/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.configurationsample.generic;

/**
 * A configuration properties that uses the builder pattern with a generic.
 *
 * @param <T> the type of the return type
 * @author Stephane Nicoll
 */
public class GenericBuilderProperties<T extends GenericBuilderProperties<T>> {

	private int number;

	public int getNumber() {
		return this.number;
	}

	@SuppressWarnings("unchecked")
	public T setNumber(int number) {
		this.number = number;
		return (T) this;
	}

}
