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

package org.springframework.boot.configurationprocessor.test;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * A tester utility for {@link RoundEnvironment}.
 *
 * @author Stephane Nicoll
 */
public class RoundEnvironmentTester {

	private final RoundEnvironment roundEnvironment;

	RoundEnvironmentTester(RoundEnvironment roundEnvironment) {
		this.roundEnvironment = roundEnvironment;
	}

	/**
	 * Return the root {@link TypeElement} for the specified {@code type}.
	 * @param type the type of the class
	 * @return the {@link TypeElement}
	 */
	public TypeElement getRootElement(Class<?> type) {
		return (TypeElement) this.roundEnvironment.getRootElements().stream()
				.filter((element) -> element.toString().equals(type.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No element found for "
						+ type
						+ " make sure it is included in the list of classes to compile"));
	}

}
