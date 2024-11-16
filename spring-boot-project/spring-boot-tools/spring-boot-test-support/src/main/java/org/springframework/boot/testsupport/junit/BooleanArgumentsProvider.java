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

package org.springframework.boot.testsupport.junit;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.platform.commons.util.Preconditions;

/**
 * An {@link ArgumentsProvider} that provides {@code true} and {@code false} values.
 *
 * @author Scott Frederick
 */
class BooleanArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		Method testMethod = context.getRequiredTestMethod();
		Preconditions.condition(testMethod.getParameterCount() > 0, () -> String.format(
				"@BooleanValueSource cannot provide arguments to method [%s]: the method does not declare any formal parameters.",
				testMethod.toGenericString()));

		return Stream.of(Arguments.arguments(false), Arguments.arguments(true));
	}

}
