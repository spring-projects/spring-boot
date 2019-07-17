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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import javax.annotation.processing.ProcessingEnvironment;

import org.springframework.boot.configurationprocessor.fieldvalues.AbstractFieldValuesProcessorTests;
import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;

import static org.junit.Assume.assumeNoException;

/**
 * Tests for {@link JavaCompilerFieldValuesParser}.
 *
 * @author Phillip Webb
 */
public class JavaCompilerFieldValuesProcessorTests extends AbstractFieldValuesProcessorTests {

	@Override
	protected FieldValuesParser createProcessor(ProcessingEnvironment env) {
		try {
			return new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			assumeNoException(ex);
			throw new IllegalStateException();
		}
	}

}
